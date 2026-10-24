package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.erezschatz.resonancedrift.ampache.AmpacheException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.random.Random

/** AlbumShuffle against a fake queue and a fake library (task 12). */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumShuffleTest {

    @get:Rule val folder = TemporaryFolder()

    /** A queue of (album id, song id) pairs; the player's position is [currentIndex]. */
    private class FakeQueue : QueueController {
        val items = mutableListOf<Pair<Long, String>>()
        override var currentIndex = 0
        var listener: (() -> Unit)? = null
        override val itemCount get() = items.size
        override fun albumIdAt(index: Int) = items.getOrNull(index)?.first

        override fun append(items: List<MediaItem>) {
            this.items += items.map { albumOf(it) to it.mediaId }
            listener?.invoke()
        }

        override fun replace(items: List<MediaItem>) {
            this.items.clear()
            this.items += items.map { albumOf(it) to it.mediaId }
            currentIndex = 0
            listener?.invoke()
        }

        /** Song ids in the fake are "<album>-<n>". */
        private fun albumOf(item: MediaItem) = item.mediaId.substringBefore("-").toLong()
    }

    /** Albums 1..[albumCount], [songsPerAlbum] songs each; [failures] next fetches fail. */
    private class FakeLibrary(val albumCount: Int, val songsPerAlbum: Int = 3) : ShuffleLibrary {
        val history = mutableListOf<Long>()
        val fetched = mutableListOf<Long>()
        var failures = 0

        override suspend fun albumPool() = (1L..albumCount).toList()

        override suspend fun albumSongs(albumId: Long): List<MediaItem> {
            if (failures > 0) {
                failures--
                throw AmpacheException.Unreachable(IOException("offline"))
            }
            fetched += albumId
            return (1..songsPerAlbum).map { MediaItem.Builder().setMediaId("$albumId-$it").build() }
        }

        override suspend fun recentAlbumIds() = history.toList()
        override suspend fun recordStarted(albumId: Long) {
            history += albumId
        }
    }

    private val queue = FakeQueue()
    private val states = mutableListOf<ShuffleState>()

    private fun TestScope.shuffle(library: FakeLibrary): AlbumShuffle {
        val log = PlaybackLog(folder.newFolder())
        val shuffle = AlbumShuffle(queue, library, log, this, onStateChanged = { state, _ -> states += state }, random = Random(7))
        // What the player would report after the queue changes.
        queue.listener = { shuffle.onTimelineChanged(Timeline.EMPTY, 0) }
        return shuffle
    }

    private fun AlbumShuffle.moveTo(index: Int) {
        queue.currentIndex = index
        onMediaItemTransition(null, 0)
    }

    @Test
    fun `shuffle all plays one album and records it as started`() = runTest {
        val library = FakeLibrary(albumCount = 10)
        val shuffle = shuffle(library)

        shuffle.start(albumId = null)
        runCurrent()
        shuffle.moveTo(0)
        runCurrent()

        assertEquals(ShuffleState.ON, shuffle.state)
        assertEquals(listOf(ShuffleState.STARTING, ShuffleState.ON), states)
        assertEquals(3, queue.itemCount)
        assertEquals(listOf(queue.albumIdAt(0)), library.history)
    }

    @Test
    fun `appends the next album at the last-but-one song and not before`() = runTest {
        val library = FakeLibrary(albumCount = 10)
        val shuffle = shuffle(library)
        shuffle.start(albumId = 4)
        runCurrent()

        assertEquals("first song: nothing appended yet", 3, queue.itemCount)
        shuffle.moveTo(1)
        runCurrent()
        assertEquals("last-but-one: the next album is queued", 6, queue.itemCount)
        assertTrue("a different album", queue.albumIdAt(3) != 4L)

        shuffle.moveTo(2)
        runCurrent()
        assertEquals("no second append while the new album is still ahead", 6, queue.itemCount)
    }

    @Test
    fun `never picks an album from the history or the one playing`() = runTest {
        val library = FakeLibrary(albumCount = 5, songsPerAlbum = 1)
        library.history += listOf(1L, 2L, 3L)
        val shuffle = shuffle(library)

        shuffle.start(albumId = 4)
        runCurrent()

        // One-song albums: the append happens at once; only album 5 is fresh.
        assertEquals(listOf(4L, 5L), (0 until queue.itemCount).map { queue.albumIdAt(it) })
    }

    @Test
    fun `when every album is in the history, history is relaxed but the playing album is still avoided`() = runTest {
        val library = FakeLibrary(albumCount = 2, songsPerAlbum = 1)
        library.history += listOf(1L, 2L)
        val shuffle = shuffle(library)

        shuffle.start(albumId = 1)
        runCurrent()

        assertEquals(listOf(1L, 2L), (0 until queue.itemCount).map { queue.albumIdAt(it) })
    }

    @Test
    fun `next album replaces the queue with a fresh album`() = runTest {
        val library = FakeLibrary(albumCount = 10)
        val shuffle = shuffle(library)
        shuffle.start(albumId = 4)
        runCurrent()

        shuffle.nextAlbum()
        runCurrent()

        assertEquals(3, queue.itemCount)
        assertTrue(queue.albumIdAt(0) != 4L)
        assertEquals(ShuffleState.ON, shuffle.state)
    }

    @Test
    fun `stop stops appending`() = runTest {
        val library = FakeLibrary(albumCount = 10)
        val shuffle = shuffle(library)
        shuffle.start(albumId = 4)
        runCurrent()

        shuffle.stop()
        shuffle.moveTo(2)
        runCurrent()

        assertEquals(ShuffleState.OFF, shuffle.state)
        assertEquals(3, queue.itemCount)
    }

    @Test
    fun `a failed fetch is retried after the delay`() = runTest {
        val library = FakeLibrary(albumCount = 10)
        val shuffle = shuffle(library)
        shuffle.start(albumId = 4)
        runCurrent()

        library.failures = 1
        shuffle.moveTo(1)
        runCurrent()
        assertEquals("the fetch failed: nothing appended", 3, queue.itemCount)

        advanceTimeBy(AlbumShuffle.RETRY_MS + 1)
        runCurrent()
        assertEquals("the retry appended the next album", 6, queue.itemCount)
        assertEquals(ShuffleState.ON, shuffle.state)
    }

    @Test
    fun `continuing keeps the queue and appends after it`() = runTest {
        val library = FakeLibrary(albumCount = 10, songsPerAlbum = 2)
        queue.items += listOf(9L to "9-1", 9L to "9-2")
        val shuffle = shuffle(library)

        shuffle.continueFromCurrent()
        runCurrent()

        assertEquals(listOf("9-1", "9-2"), queue.items.take(2).map { it.second })
        assertEquals(4, queue.itemCount)
        assertTrue(queue.albumIdAt(2) != 9L)
    }

    @Test
    fun `a start that cannot reach the server turns shuffle back off`() = runTest {
        val library = FakeLibrary(albumCount = 10)
        library.failures = 1
        val shuffle = shuffle(library)

        shuffle.start(albumId = null)
        runCurrent()

        assertEquals(ShuffleState.OFF, shuffle.state)
        assertEquals(listOf(ShuffleState.STARTING, ShuffleState.OFF), states)
        assertEquals(0, queue.itemCount)
    }
}
