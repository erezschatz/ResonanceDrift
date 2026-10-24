package net.erezschatz.resonancedrift.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.ArtistEntity
import net.erezschatz.resonancedrift.data.entity.GenreEntity
import net.erezschatz.resonancedrift.data.entity.PlaylistEntity
import net.erezschatz.resonancedrift.data.entity.SongEntity

/**
 * The cached library: albums, artists, genres, songs, playlists. Reads are
 * Flows, so a refresh that writes here reaches the screen without being told.
 * A `replace…` is one transaction, so a list is never seen half-written.
 */
@Dao
abstract class LibraryDao {

    // --- Artists

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE")
    abstract fun observeArtists(): Flow<List<ArtistEntity>>

    @Query("DELETE FROM artists")
    protected abstract suspend fun deleteArtists()

    @Upsert
    protected abstract suspend fun upsertArtists(artists: List<ArtistEntity>)

    /** The full list from the server replaces the cached one; artists gone from the server go here too. */
    @Transaction
    open suspend fun replaceArtists(artists: List<ArtistEntity>) {
        deleteArtists()
        upsertArtists(artists)
    }

    // --- Genres

    @Query("SELECT * FROM genres ORDER BY name COLLATE NOCASE")
    abstract fun observeGenres(): Flow<List<GenreEntity>>

    @Query("DELETE FROM genres")
    protected abstract suspend fun deleteGenres()

    @Upsert
    protected abstract suspend fun upsertGenres(genres: List<GenreEntity>)

    @Transaction
    open suspend fun replaceGenres(genres: List<GenreEntity>) {
        deleteGenres()
        upsertGenres(genres)
    }

    // --- Playlists

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    abstract fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("DELETE FROM playlists")
    protected abstract suspend fun deletePlaylists()

    @Upsert
    protected abstract suspend fun upsertPlaylists(playlists: List<PlaylistEntity>)

    @Transaction
    open suspend fun replacePlaylists(playlists: List<PlaylistEntity>) {
        deletePlaylists()
        upsertPlaylists(playlists)
    }

    // --- Albums

    @Query("SELECT * FROM albums WHERE id = :albumId")
    abstract fun observeAlbum(albumId: Long): Flow<AlbumEntity?>

    /** Year, then name: discography order without relying on the server's. */
    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY year, name COLLATE NOCASE")
    abstract fun observeArtistAlbums(artistId: Long): Flow<List<AlbumEntity>>

    @Upsert
    abstract suspend fun upsertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE artistId = :artistId")
    protected abstract suspend fun deleteArtistAlbums(artistId: Long)

    @Transaction
    open suspend fun replaceArtistAlbums(artistId: Long, albums: List<AlbumEntity>) {
        deleteArtistAlbums(artistId)
        upsertAlbums(albums)
    }

    // --- Songs

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY track, title COLLATE NOCASE")
    abstract fun observeAlbumSongs(albumId: Long): Flow<List<SongEntity>>

    @Upsert
    abstract suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE albumId = :albumId")
    protected abstract suspend fun deleteAlbumSongs(albumId: Long)

    /** The album row and its songs together, so the detail screen never shows one without the other. */
    @Transaction
    open suspend fun replaceAlbum(album: AlbumEntity, songs: List<SongEntity>) {
        upsertAlbums(listOf(album))
        deleteAlbumSongs(album.id)
        upsertSongs(songs)
    }

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    protected abstract suspend fun songsWithIds(songIds: List<Long>): List<SongEntity>

    /**
     * The songs for [songIds], in that order, repeats included; ids not in the
     * cache are left out. Chunked to stay under SQLite's limit on `?` parameters.
     */
    open suspend fun songsInOrder(songIds: List<Long>): List<SongEntity> {
        val byId = mutableMapOf<Long, SongEntity>()
        for (chunk in songIds.distinct().chunked(MAX_QUERY_PARAMETERS)) {
            for (song in songsWithIds(chunk)) byId[song.id] = song
        }
        return songIds.mapNotNull { byId[it] }
    }

    private companion object {
        const val MAX_QUERY_PARAMETERS = 900
    }
}
