package net.erezschatz.resonancedrift.data

import net.erezschatz.resonancedrift.ampache.model.Ref
import net.erezschatz.resonancedrift.ampache.model.Song
import net.erezschatz.resonancedrift.data.entity.GenreTag
import net.erezschatz.resonancedrift.data.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class EntitiesTest {

    private val converters = Converters()

    @Test
    fun `a song survives the trip to the cache and back`() {
        val song = Song(
            id = 14835,
            title = "Saturn Return",
            track = 10,
            time = 38,
            year = 2003,
            artist = Ref(1, "Artist"),
            albumartist = Ref(2, "Album Artist"),
            album = Ref(3, "Album"),
            genre = listOf(Ref(4, "Rock"), Ref(5, "Folk")),
        )
        // art is the server's token-bearing URL; it is deliberately not cached.
        assertEquals(song, SongEntity.from(song).toSong())
    }

    @Test
    fun `missing refs stay missing`() {
        val song = Song(id = 1, title = "Loose")
        val entity = SongEntity.from(song)
        assertEquals(null, entity.albumId)
        assertEquals("", entity.albumName)
        assertEquals(song, entity.toSong())
    }

    @Test
    fun `id lists round-trip, including empty and repeats`() {
        for (ids in listOf(emptyList(), listOf(7L), listOf(3L, 1L, 3L, 70098L))) {
            assertEquals(ids, converters.textToIds(converters.idsToText(ids)))
        }
        assertEquals("3,1,3", converters.idsToText(listOf(3L, 1L, 3L)))
    }

    @Test
    fun `genre lists round-trip`() {
        val genres = listOf(GenreTag(4, "Rock"), GenreTag(5, "Folk, \"live\""))
        assertEquals(genres, converters.textToGenres(converters.genresToText(genres)))
        assertEquals(emptyList<GenreTag>(), converters.textToGenres(converters.genresToText(emptyList())))
    }
}
