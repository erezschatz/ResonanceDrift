package net.erezschatz.resonancedrift.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.erezschatz.resonancedrift.ampache.model.Ref
import net.erezschatz.resonancedrift.ampache.model.Song
import net.erezschatz.resonancedrift.data.entity.GenreTag
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Bundles need a device, hence instrumented: Now Playing's links survive the MediaItem. */
@RunWith(AndroidJUnit4::class)
class MediaItemsTest {

    @Test
    fun linksRoundTripThroughTheMetadataExtras() {
        val song = Song(
            id = 1,
            title = "Song",
            year = 2003,
            artist = Ref(7, "Artist"),
            album = Ref(70, "Artist - Album"),
            genre = listOf(Ref(4, "Rock"), Ref(5, "Folk")),
        )

        val metadata = MediaItems.fromSong(song, login = null).mediaMetadata

        assertEquals(MediaItems.Links(70, 7, listOf(GenreTag(4, "Rock"), GenreTag(5, "Folk"))), MediaItems.linksOf(metadata))
        assertEquals("prefix stripped for the notification", "Album", metadata.albumTitle)
        assertEquals(2003, metadata.releaseYear)
    }

    @Test
    fun aSongWithoutRefsHasNoLinks() {
        val metadata = MediaItems.fromSong(Song(id = 2, title = "Loose"), login = null).mediaMetadata

        assertEquals(MediaItems.Links(null, null, emptyList()), MediaItems.linksOf(metadata))
    }
}
