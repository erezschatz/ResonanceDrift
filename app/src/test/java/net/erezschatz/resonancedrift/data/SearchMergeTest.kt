package net.erezschatz.resonancedrift.data

import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchMergeTest {

    @Test
    fun `title albums first, then artist albums, then the albums of matching songs, each once`() {
        val merged = LibraryRepository.mergeAlbums(
            byTitle = listOf(album(1), album(2)),
            byArtist = listOf(album(2), album(3)),
            songs = listOf(song(10, albumId = 3), song(11, albumId = 4), song(12, albumId = 4), song(13, albumId = null)),
        )

        assertEquals(listOf(1L, 2L, 3L, 4L), merged.map { it.id })
    }

    @Test
    fun `an album made from a song carries the song's album name, album artist and year`() {
        val merged = LibraryRepository.mergeAlbums(emptyList(), emptyList(), listOf(song(10, albumId = 4)))

        assertEquals(AlbumEntity(4, "Album 4", 20, "Album Artist", 1999, 0, 0), merged.single())
    }

    @Test
    fun `merged albums are capped at fifty`() {
        val merged = LibraryRepository.mergeAlbums((1L..40L).map { album(it) }, (30L..80L).map { album(it) }, emptyList())

        assertEquals(LibraryRepository.SEARCH_ALBUM_LIMIT, merged.size)
        assertEquals((1L..50L).toList(), merged.map { it.id })
    }

    private fun album(id: Long) = AlbumEntity(id, "Album $id", 7, "Artist", 2003, 10, 2400)

    private fun song(id: Long, albumId: Long?) = SongEntity(
        id = id,
        title = "Song $id",
        track = 1,
        durationSeconds = 200,
        year = 1999,
        artistId = 7,
        artistName = "Artist",
        albumArtistId = 20,
        albumArtistName = "Album Artist",
        albumId = albumId,
        albumName = "Album $albumId",
        genres = emptyList(),
    )
}
