package net.erezschatz.resonancedrift.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.erezschatz.resonancedrift.playback.PlaybackService
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

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

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTitle = MutableStateFlow<String?>(null)
    val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    /** Main thread. Binding creates the service if it is not running. */
    fun connect() {
        if (controllerFuture != null) return
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
            publish(connectedController)
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        _connected.value = false
    }

    /** Replace the queue with [items] and play from the first. */
    fun playQueue(items: List<MediaItem>) {
        val current = controller ?: return
        current.setMediaItems(items)
        current.prepare()
        current.play()
    }

    fun togglePlayPause() {
        val current = controller ?: return
        if (current.isPlaying) current.pause() else current.play()
    }

    private fun publish(player: Player) {
        _isPlaying.value = player.isPlaying
        _currentTitle.value = player.mediaMetadata.title?.toString()
    }
}
