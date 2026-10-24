package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.ShuffleHistory
import kotlin.random.Random

/** What the UI is told (session extras, Commands.kt). */
enum class ShuffleState { OFF, STARTING, ON }

/** Where AlbumShuffle gets albums and keeps its history; a fake in the tests. */
interface ShuffleLibrary {
    suspend fun albumPool(): List<Long>
    suspend fun albumSongs(albumId: Long): List<MediaItem>
    suspend fun recentAlbumIds(): List<Long>
    suspend fun recordStarted(albumId: Long)
}

/**
 * Whole-album random play, in the service (task 12). When the current item is
 * the last or last-but-one, the next album — not among the last 50 played, not
 * the one playing — is fetched and appended, so the boundary is an ordinary
 * gapless transition that needs no UI and no awake screen.
 *
 * It talks to the player only through [QueueController]. Main thread
 * throughout ([scope] is the service's). A failed fetch is retried after
 * [RETRY_MS] and on the next transition; it never stops playback.
 */
class AlbumShuffle(
    private val queue: QueueController,
    private val library: ShuffleLibrary,
    private val log: PlaybackLog,
    private val scope: CoroutineScope,
    private val onStateChanged: (ShuffleState, message: String?) -> Unit,
    private val random: Random = Random.Default,
) : Player.Listener {

    var state = ShuffleState.OFF
        private set

    /** Loaded once per service run: the whole library's album ids. */
    private var pool: List<Long>? = null

    private var starting: Job? = null
    private var fetching: Job? = null
    private var retry: Job? = null
    private var lastStartedAlbum: Long? = null

    /** Shuffle all (albumId null) or "shuffle from this album": the queue becomes that album. */
    fun start(albumId: Long?) {
        cancelJobs()
        setState(ShuffleState.STARTING, null)
        starting = scope.launch {
            try {
                val id = albumId ?: pickNext(exclude = emptySet())
                val items = library.albumSongs(id)
                if (items.isEmpty()) throw NoAlbumException("The album has no songs.")
                lastStartedAlbum = null
                // ON before replacing, so the transition that follows records the album as started.
                setState(ShuffleState.ON, null)
                queue.replace(items)
                log.log("shuffle: started album $id (${items.size} songs)")
            } catch (e: AmpacheException) {
                failStart(e.userMessage)
            } catch (e: NoAlbumException) {
                failStart(e.message.orEmpty())
            }
        }
    }

    /** Now Playing's shuffle button: keep the queue, and carry on with random albums after it. */
    fun continueFromCurrent() {
        if (state == ShuffleState.ON) return
        cancelJobs()
        lastStartedAlbum = queue.albumIdAt(queue.currentIndex)
        setState(ShuffleState.ON, null)
        log.log("shuffle: on, continuing after the current queue")
        maybeAppend()
    }

    /** Next album: a fresh random album now, shuffle on. */
    fun nextAlbum() = start(albumId = null)

    fun stop() {
        if (state == ShuffleState.OFF) return
        cancelJobs()
        setState(ShuffleState.OFF, null)
        log.log("shuffle: off")
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        recordAlbumStart()
        maybeAppend()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) = maybeAppend()

    /** History is written when an album starts playing, not when it is queued (task 12). */
    private fun recordAlbumStart() {
        if (state != ShuffleState.ON) return
        val album = queue.albumIdAt(queue.currentIndex) ?: return
        if (album == lastStartedAlbum) return
        lastStartedAlbum = album
        scope.launch { library.recordStarted(album) }
    }

    private fun maybeAppend() {
        if (state != ShuffleState.ON) return
        if (fetching?.isActive == true || retry?.isActive == true) return
        if (queue.itemCount == 0 || queue.currentIndex < queue.itemCount - 2) return
        fetching = scope.launch {
            try {
                val id = pickNext(exclude = albumsInQueue())
                val items = library.albumSongs(id)
                if (items.isEmpty()) throw NoAlbumException("album $id has no songs")
                if (state != ShuffleState.ON) return@launch
                queue.append(items)
                log.log("shuffle: queued album $id (${items.size} songs)")
            } catch (e: AmpacheException) {
                scheduleRetry(e.userMessage)
            } catch (e: NoAlbumException) {
                scheduleRetry(e.message.orEmpty())
            }
        }
    }

    private fun scheduleRetry(why: String) {
        log.log("shuffle: next album not fetched ($why); retrying in ${RETRY_MS / 1000} s")
        retry = scope.launch {
            delay(RETRY_MS)
            retry = null
            maybeAppend()
        }
    }

    /** A random album from the pool, avoiding recent history and [exclude]; history is relaxed before giving up. */
    private suspend fun pickNext(exclude: Set<Long>): Long {
        val all = pool ?: library.albumPool().also { pool = it }
        val recent = library.recentAlbumIds().toSet()
        val fresh = all.filter { it !in recent && it !in exclude }
        val candidates = fresh.ifEmpty { all.filter { it !in exclude } }
        if (candidates.isEmpty()) throw NoAlbumException("The library has no albums to shuffle.")
        return candidates[random.nextInt(candidates.size)]
    }

    private fun albumsInQueue(): Set<Long> =
        (0 until queue.itemCount).mapNotNull { queue.albumIdAt(it) }.toSet()

    private fun failStart(message: String) {
        log.log("shuffle: could not start ($message)")
        setState(ShuffleState.OFF, message)
    }

    private fun setState(newState: ShuffleState, message: String?) {
        state = newState
        onStateChanged(newState, message)
    }

    private fun cancelJobs() {
        starting?.cancel()
        fetching?.cancel()
        retry?.cancel()
        starting = null
        fetching = null
        retry = null
    }

    private class NoAlbumException(message: String) : Exception(message)

    companion object {
        /** Long enough not to hammer a dead network, short enough to beat the end of a typical song. */
        const val RETRY_MS = 30_000L
    }
}

/** [ShuffleLibrary] over the app's repository and history. */
class AppShuffleLibrary(
    private val library: LibraryRepository,
    private val history: ShuffleHistory,
    private val session: Session,
) : ShuffleLibrary {
    override suspend fun albumPool(): List<Long> = library.albumPool()

    override suspend fun albumSongs(albumId: Long): List<MediaItem> {
        val login = session.currentLogin()
        return library.albumSongsForPlayback(albumId).map { MediaItems.fromSong(it.toSong(), login) }
    }

    override suspend fun recentAlbumIds(): List<Long> = history.recentAlbumIds()

    override suspend fun recordStarted(albumId: Long) = history.add(albumId)
}
