package net.erezschatz.resonancedrift.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.ArtistEntity
import net.erezschatz.resonancedrift.data.entity.GenreTag
import net.erezschatz.resonancedrift.data.entity.PlaylistEntity
import net.erezschatz.resonancedrift.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDaoTest {

    @get:Rule val rule = DbTestRule()
    private val dao get() = rule.db.library()

    @Test
    fun artistsReplaceDropsRowsGoneFromTheServerAndSortsByName() = runTest {
        dao.replaceArtists(listOf(artist(1, "beta"), artist(2, "Alpha"), artist(3, "Gone")))
        dao.replaceArtists(listOf(artist(1, "beta"), artist(2, "Alpha")))

        assertEquals(listOf("Alpha", "beta"), dao.observeArtists().first().map { it.name })
    }

    @Test
    fun playlistsRoundTripWithStringIds() = runTest {
        val smart = PlaylistEntity("smart_3", "Recent", 50, "erez", "public")
        dao.replacePlaylists(listOf(smart, PlaylistEntity("12", "Drive", 2000, "erez", "private")))

        assertEquals(smart, dao.observePlaylists().first().first { it.id == "smart_3" })
    }

    @Test
    fun artistAlbumsInYearThenNameOrder() = runTest {
        dao.replaceArtistAlbums(7, listOf(album(1, "Later", 7, 2010), album(2, "b side", 7, 2001), album(3, "A side", 7, 2001)))
        dao.upsertAlbums(listOf(album(4, "Someone else's", 8, 1999)))

        assertEquals(listOf(3L, 2L, 1L), dao.observeArtistAlbums(7).first().map { it.id })
    }

    @Test
    fun albumAndSongsReplaceTogetherAndSongsKeepEveryField() = runTest {
        val first = song(1, albumId = 5, track = 2)
        dao.replaceAlbum(album(5, "Album", 7, 2003), listOf(first, song(2, albumId = 5, track = 1), song(3, albumId = 5, track = 3)))
        dao.replaceAlbum(album(5, "Album (remaster)", 7, 2003), listOf(first, song(2, albumId = 5, track = 1)))

        assertEquals("Album (remaster)", dao.observeAlbum(5).first()?.name)
        val songs = dao.observeAlbumSongs(5).first()
        assertEquals(listOf(2L, 1L), songs.map { it.id })
        assertEquals(first, songs.last())
    }

    @Test
    fun songsInOrderKeepsOrderAndRepeatsAndSkipsUnknownIds() = runTest {
        dao.upsertSongs((1L..2000L).map { song(it, albumId = 1, track = 0) })
        val ids = (2000L downTo 1L).toList() + listOf(5L, 99999L, 5L)

        val songs = dao.songsInOrder(ids)

        assertEquals(ids.filter { it != 99999L }, songs.map { it.id })
    }

    private fun artist(id: Long, name: String) = ArtistEntity(id, name, 1, 10)

    private fun album(id: Long, name: String, artistId: Long, year: Int) =
        AlbumEntity(id, name, artistId, "Artist $artistId", year, 10, 2400)

    private fun song(id: Long, albumId: Long, track: Int) = SongEntity(
        id = id,
        title = "Song $id",
        track = track,
        durationSeconds = 200,
        year = 2003,
        artistId = 7,
        artistName = "Artist",
        albumArtistId = null,
        albumArtistName = "",
        albumId = albumId,
        albumName = "Album",
        genres = listOf(GenreTag(4, "Rock")),
    )
}
