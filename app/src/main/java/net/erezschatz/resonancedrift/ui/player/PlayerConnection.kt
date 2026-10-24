package net.erezschatz.resonancedrift.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.StoredLogin
import net.erezschatz.resonancedrift.data.entity.SongEntity
import net.erezschatz.resonancedrift.playback.MediaItems
import net.erezschatz.resonancedrift.playback.PlaybackEvents
import net.erezschatz.resonancedrift.playback.PlaybackService
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/** What the mini player and Now Playing show for one queue entry. */
data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    /** For album art (ui/components/AlbumArt.kt); null when the item has none. */
    val albumId: Long?,
)

/** Where playback is in the current item, for a seek bar. Duration 0 = not known yet. */
data class Position(val positionMs: Long, val durationMs: Long)

/**
 * The UI's only handle on playback: a MediaController to the service, with
 * what the screens show exposed as StateFlows. It sends commands and reports
 * state; it decides nothing. Lives in AppGraph so the flows outlive any one
 * screen; MainActivity connects it in onStart and disconnects in onStop, so
 * the service is not kept bound while the app is in the background.
 */
class PlayerConnection(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Lives while connected; only the position ticker runs in it. */
    private var scope: CoroutineScope? = null
    private var ticker: Job? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Asked to play but waiting for audio (a song starting, or the buffer ran dry): the UI shows a spinner. */
    private val _isWaiting = MutableStateFlow(false)
    val isWaiting: StateFlow<Boolean> = _isWaiting.asStateFlow()

    /** The item playing (or paused on); null when the queue is empty. */
    private val _current = MutableStateFlow<QueueEntry?>(null)
    val current: StateFlow<QueueEntry?> = _current.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _position = MutableStateFlow(Position(0, 0))
    val position: StateFlow<Position> = _position.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) publishQueue(player)
            publish(player)
        }
    }

    /** Main thread. Binding creates the service if it is not running. */
    fun connect() {
        if (controllerFuture != null) return
        scope = MainScope()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val connectedController = try {
                future.get()
            } catch (e: ExecutionException) {
                return@addListener
            } catch (e: CancellationException) {
                // disconnect() ran before the connection completed
                return@addListener
            }
            controller = connectedController
            connectedController.addListener(listener)
            _connected.value = true
            publishQueue(connectedController)
            publish(connectedController)
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        scope?.cancel()
        scope = null
        ticker = null
        _connected.value = false
        _isPlaying.value = false
        _isWaiting.value = false
    }

    /** Replace the queue with [items] and play from [startIndex]. */
    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        val current = controller ?: return
        current.setMediaItems(items, startIndex, 0L)
        current.prepare()
        current.play()
    }

    /** The screens' way to play: [songs] as the queue, from [startIndex]. */
    fun playSongs(songs: List<SongEntity>, startIndex: Int, login: StoredLogin?) {
        playQueue(songs.map { MediaItems.fromSong(it.toSong(), login) }, startIndex)
    }

    /** By playWhenReady, not isPlaying: a tap while a song is still starting pauses it. */
    fun togglePlayPause() {
        val current = controller ?: return
        if (current.playWhenReady) current.pause() else current.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /** One line of player state for the diagnostics report. */
    fun describe(): String {
        val current = controller ?: return "player: not connected"
        val error = current.playerError?.let { ", error ${it.errorCodeName}" }.orEmpty()
        return "player: ${PlaybackEvents.stateName(current.playbackState)}" +
            (if (current.isPlaying) ", playing" else ", not playing") +
            ", playWhenReady ${current.playWhenReady}" +
            ", item ${current.currentMediaItemIndex + 1}/${current.mediaItemCount}" +
            " song ${current.currentMediaItem?.mediaId}" +
            ", at ${current.currentPosition / 1000} s, buffered to ${current.bufferedPosition / 1000} s" +
            error
    }

    private fun publish(player: Player) {
        _isPlaying.value = player.isPlaying
        _isWaiting.value = player.playWhenReady && player.playbackState == Player.STATE_BUFFERING
        _current.value = player.currentMediaItem?.let { entryFor(it) }
        _currentIndex.value = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
        publishPosition(player)
        // The controller reports state changes, not the clock; while playing the
        // position is read twice a second for the seek bar. Display only.
        if (player.isPlaying) startTicker() else stopTicker()
    }

    private fun publishQueue(player: Player) {
        _queue.value = (0 until player.mediaItemCount).map { entryFor(player.getMediaItemAt(it)) }
    }

    private fun publishPosition(player: Player) {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _position.value = Position(player.currentPosition.coerceAtLeast(0), duration)
    }

    private fun startTicker() {
        if (ticker != null) return
        ticker = scope?.launch {
            while (isActive) {
                delay(POSITION_REFRESH_MS)
                controller?.let { publishPosition(it) }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun entryFor(item: MediaItem): QueueEntry {
        val metadata = item.mediaMetadata
        return QueueEntry(
            mediaId = item.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            albumId = MediaItems.albumIdOf(metadata),
        )
    }

    private companion object {
        const val POSITION_REFRESH_MS = 500L
    }
}
