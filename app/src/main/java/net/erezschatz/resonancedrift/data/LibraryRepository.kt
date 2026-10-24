package net.erezschatz.resonancedrift.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AmpacheClient
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.data.dao.LibraryDao
import net.erezschatz.resonancedrift.data.dao.PlaylistSongsDao
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.ArtistEntity
import net.erezschatz.resonancedrift.data.entity.GenreEntity
import net.erezschatz.resonancedrift.data.entity.PlaylistEntity
import net.erezschatz.resonancedrift.data.entity.SongEntity
import kotlin.random.Random

/** Where a list's background refresh stands. */
sealed interface Refresh {
    data object Running : Refresh
    data object Done : Refresh

    /** The cached items are still shown; [error] has the user-facing message. */
    data class Failed(val error: AmpacheException) : Refresh
}

/** What a list screen renders: the cached items, and whether they are being refreshed. */
data class Listing<T>(val items: List<T>, val refresh: Refresh)

/**
 * The only thing ViewModels read the library from (docs/SPEC.md → Data).
 * Lists are cache-first: the cached rows come out at once, the server is asked
 * in the background, the answer is written to Room, and Room's Flow brings it
 * to the screen. Every refresh writes the server's whole answer for that list,
 * so rows added, changed or removed on the server are added, changed or
 * removed here too. The Albums page and Genre detail are API-only, as in ReDrift.
 */
class LibraryRepository(
    private val session: Session,
    private val client: AmpacheClient,
    private val library: LibraryDao,
    private val playlistSongs: PlaylistSongsDao,
    private val random: Random = Random.Default,
) {

    // --- Cache-first lists

    fun artists(): Flow<Listing<ArtistEntity>> =
        cacheThenRefresh(library.observeArtists()) {
            val all = AmpacheClient.fetchAllPages { offset, limit ->
                session.call { server, token -> client.artists(server, token, offset, limit) }
            }
            library.replaceArtists(all.map { ArtistEntity.from(it) })
        }

    fun genres(): Flow<Listing<GenreEntity>> =
        cacheThenRefresh(library.observeGenres()) {
            val all = AmpacheClient.fetchAllPages { offset, limit ->
                session.call { server, token -> client.genres(server, token, offset, limit) }
            }
            library.replaceGenres(all.map { GenreEntity.from(it) })
        }

    fun playlists(): Flow<Listing<PlaylistEntity>> =
        cacheThenRefresh(library.observePlaylists()) {
            val all = AmpacheClient.fetchAllPages { offset, limit ->
                session.call { server, token -> client.playlists(server, token, offset, limit) }
            }
            library.replacePlaylists(all.map { PlaylistEntity.from(it) })
        }

    fun artistAlbums(artistId: Long): Flow<Listing<AlbumEntity>> =
        cacheThenRefresh(library.observeArtistAlbums(artistId)) {
            val albums = session.call { server, token -> client.artistAlbums(server, token, artistId) }
            library.replaceArtistAlbums(artistId, albums.map { AlbumEntity.from(it) })
        }

    /** The album row itself; [albumSongs] refreshes it along with the songs. */
    fun album(albumId: Long): Flow<AlbumEntity?> = library.observeAlbum(albumId)

    fun albumSongs(albumId: Long): Flow<Listing<SongEntity>> =
        cacheThenRefresh(library.observeAlbumSongs(albumId)) {
            val album = session.call { server, token -> client.album(server, token, albumId) }
            val songs = session.call { server, token -> client.albumSongs(server, token, albumId) }
            library.replaceAlbum(AlbumEntity.from(album), songs.map { SongEntity.from(it) })
        }

    /**
     * A playlist's songs in playlist order. The refresh writes only if no
     * removal happened while it was fetching (PlaylistSongsDao's version counter).
     * When one did, it fetches once more: the removal reached the server before
     * the cache, so the second answer already reflects it.
     */
    fun playlistSongs(playlistId: String): Flow<Listing<SongEntity>> {
        val cached = playlistSongs.observe(playlistId).map { row ->
            if (row == null) emptyList() else library.songsInOrder(row.songIds)
        }
        return cacheThenRefresh(cached) {
            for (attempt in 1..PLAYLIST_FETCH_ATTEMPTS) {
                val versionAtStart = playlistSongs.versionOf(playlistId)
                val songs = session.call { server, token -> client.playlistSongs(server, token, playlistId) }
                // Songs first: the playlist row's Flow reads them as soon as the row changes.
                library.upsertSongs(songs.map { SongEntity.from(it) })
                if (playlistSongs.replaceIfUnchanged(playlistId, versionAtStart, songs.map { it.id })) break
            }
        }
    }

    /**
     * Removes the song at [index] on the server, then from the cache. Throws
     * [AmpacheException] if the server refused; the cache is then untouched.
     */
    suspend fun removeFromPlaylist(playlistId: String, index: Int, songId: Long) {
        session.call { server, token -> client.playlistRemoveSong(server, token, playlistId, songId) }
        playlistSongs.removeSong(playlistId, index, songId)
    }

    // --- API-only (each call is a fresh answer; results are still cached for detail screens)

    /**
     * A page of albums at a random offset, so the Albums screen differs each
     * time (ReDrift's behaviour). The offset range comes from the handshake's
     * album count; unknown (0) means the first page.
     */
    suspend fun randomAlbums(count: Int = RANDOM_PAGE_SIZE): List<AlbumEntity> {
        val total = session.currentLogin()?.albumCount ?: 0
        val offset = randomOffset(total, count, random)
        val albums = session.call { server, token -> client.albums(server, token, offset, count) }
            .map { AlbumEntity.from(it) }
        library.upsertAlbums(albums)
        return albums
    }

    /** Genre detail's grid: year, then name (docs/SPEC.md → Scope). */
    suspend fun genreAlbums(genreId: Long): List<AlbumEntity> {
        val albums = session.call { server, token -> client.genreAlbums(server, token, genreId) }
            .map { AlbumEntity.from(it) }
        library.upsertAlbums(albums)
        return albums.sortedWith(compareBy<AlbumEntity> { it.year }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    companion object {
        /** A second fetch covers a removal made during the first; more would only chase the user's taps. */
        const val PLAYLIST_FETCH_ATTEMPTS = 2

        /** ReDrift showed 100 albums per random page. */
        const val RANDOM_PAGE_SIZE = 100

        /** An offset that leaves a full page after it when the library is big enough. */
        fun randomOffset(total: Int, count: Int, random: Random): Int {
            val maxOffset = total - count
            return if (maxOffset > 0) random.nextInt(maxOffset + 1) else 0
        }

        /**
         * Emits [cached] as it changes, each time paired with the state of one
         * background [refresh] started on collection. The refresh writes to the
         * database, and [cached] — a Room Flow — carries the result out.
         * A failed refresh keeps the cached items and reports the error.
         *
         * Room re-runs its query a moment after a write, so `Done` is not taken
         * from that: the list is read once more after the refresh, and only
         * then is the refresh reported finished, with those items.
         */
        fun <T> cacheThenRefresh(cached: Flow<List<T>>, refresh: suspend () -> Unit): Flow<Listing<T>> =
            channelFlow {
                val items = MutableStateFlow<List<T>?>(null)
                val status = MutableStateFlow<Refresh>(Refresh.Running)
                launch { cached.collect { items.value = it } }
                launch {
                    val result = try {
                        refresh()
                        Refresh.Done
                    } catch (e: AmpacheException) {
                        Refresh.Failed(e)
                    }
                    items.value = cached.first()
                    status.value = result
                }
                items.filterNotNull().combine(status) { current, refresh -> Listing(current, refresh) }
                    .collect { send(it) }
            }
    }
}
