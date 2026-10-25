package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.erezschatz.resonancedrift.ampache.AmpacheException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/** record_play once per song start (task 13), driven by the listener calls the player would make. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrobblerTest {

    @get:Rule val folder = TemporaryFolder()

    private val recorded = mutableListOf<Long>()
    private lateinit var log: PlaybackLog

    private fun TestScope.scrobbler(fail: Boolean = false): Scrobbler {
        log = PlaybackLog(folder.newFolder())
        return Scrobbler(
            record = { songId ->
                if (fail) throw AmpacheException.Unreachable(IOException("offline"))
                recorded += songId
            },
            log,
            this,
        )
    }

    private fun Scrobbler.moveTo(songId: Long, reason: Int = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) =
        onMediaItemTransition(MediaItem.Builder().setMediaId(songId.toString()).build(), reason)

    @Test
    fun `a screen's Play counts its song once playback starts`() = runTest {
        val scrobbler = scrobbler()
        scrobbler.moveTo(10, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        runCurrent()
        assertEquals("queued, not yet playing", emptyList<Long>(), recorded)

        scrobbler.onIsPlayingChanged(true)
        runCurrent()
        assertEquals(listOf(10L), recorded)
    }

    @Test
    fun `songs running into each other each count once`() = runTest {
        val scrobbler = scrobbler()
        scrobbler.moveTo(10, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        scrobbler.onIsPlayingChanged(true)
        scrobbler.moveTo(11)
        scrobbler.moveTo(12)
        runCurrent()
        assertEquals(listOf(10L, 11L, 12L), recorded)
    }

    @Test
    fun `pause and resume is not another play`() = runTest {
        val scrobbler = scrobbler()
        scrobbler.moveTo(10)
        scrobbler.onIsPlayingChanged(true)
        scrobbler.onIsPlayingChanged(false)
        scrobbler.onIsPlayingChanged(true)
        runCurrent()
        assertEquals(listOf(10L), recorded)
    }

    @Test
    fun `a new queue that keeps the playing song does not count it again`() = runTest {
        val scrobbler = scrobbler()
        scrobbler.moveTo(10)
        scrobbler.onIsPlayingChanged(true)
        scrobbler.moveTo(10, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        runCurrent()
        assertEquals(listOf(10L), recorded)
    }

    @Test
    fun `repeat of the same song counts again`() = runTest {
        val scrobbler = scrobbler()
        scrobbler.moveTo(10)
        scrobbler.onIsPlayingChanged(true)
        scrobbler.moveTo(10, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
        runCurrent()
        assertEquals(listOf(10L, 10L), recorded)
    }

    @Test
    fun `a restored song resumed is not counted`() = runTest {
        val scrobbler = scrobbler()
        scrobbler.moveTo(10, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        scrobbler.skipCurrent()
        scrobbler.onIsPlayingChanged(true)
        scrobbler.moveTo(11)
        runCurrent()
        assertEquals(listOf(11L), recorded)
    }

    @Test
    fun `a failure is logged and playback carries on counting`() = runTest {
        val scrobbler = scrobbler(fail = true)
        scrobbler.moveTo(10)
        scrobbler.onIsPlayingChanged(true)
        runCurrent()
        assertTrue(log.entries().any { it.text.startsWith("record_play song-10 failed") })
    }
}
