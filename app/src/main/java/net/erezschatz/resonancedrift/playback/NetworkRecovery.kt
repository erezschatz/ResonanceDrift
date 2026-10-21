package net.erezschatz.resonancedrift.playback

import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session

/**
 * Layer 2 of recovery (layer 1 is [RetryingLoadErrorPolicy]): when the player
 * has stopped with an error anyway, bring it back where it was. Policy in
 * [RecoveryPolicy]; this class only wires it to the player, the session and
 * the network callback. Main thread throughout ([scope] is the service's).
 *
 * `prepare()` on an errored player keeps the playlist, the item, the position
 * and playWhenReady — so a pause pressed during an outage is respected.
 * It never skips to the next item.
 */
class NetworkRecovery(
    private val player: Player,
    private val session: Session,
    private val log: PlaybackLog,
    private val connectivity: ConnectivityManager,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var attempt = 0
    private var reauthTried = false
    private var pending: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            log.log("network: available")
            if (hasFailed()) retryNow("network back")
        }

        override fun onLost(network: Network) = log.log("network: lost")
    }

    fun start() {
        player.addListener(this)
        // Callbacks on the main thread, like every other player interaction.
        connectivity.registerDefaultNetworkCallback(networkCallback, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        pending?.cancel()
        player.removeListener(this)
        connectivity.unregisterNetworkCallback(networkCallback)
    }

    override fun onPlayerError(error: PlaybackException) {
        if (session.currentLogin() == null) {
            log.log("recovery: logged out, not retrying")
            return
        }
        val action = RecoveryPolicy.classify(error.errorCode, RecoveryPolicy.httpStatusOf(error), reauthTried)
        when (action) {
            RecoveryPolicy.Action.RETRY -> scheduleRetry()
            RecoveryPolicy.Action.REAUTH -> reauthenticateThenRetry()
            RecoveryPolicy.Action.GIVE_UP -> log.log("recovery: ${error.errorCodeName} is not recoverable; stopped in place")
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY && (attempt > 0 || reauthTried)) {
            log.log("recovery: playing again after ${attempt} attempt(s)")
            attempt = 0
            reauthTried = false
            pending?.cancel()
        }
    }

    private fun scheduleRetry() {
        if (attempt >= RecoveryPolicy.RECOVERY_ATTEMPTS) {
            log.log("recovery: giving up after $attempt attempts")
            return
        }
        val delayMs = RecoveryPolicy.recoveryDelayMs(attempt)
        attempt++
        log.log("recovery: attempt $attempt in ${delayMs / 1000} s (or when the network returns)")
        pending?.cancel()
        pending = scope.launch {
            delay(delayMs)
            retryNow("backoff")
        }
    }

    private fun reauthenticateThenRetry() {
        reauthTried = true
        log.log("recovery: stream refused; renewing the session token")
        pending?.cancel()
        pending = scope.launch {
            try {
                session.renewToken()
                log.log("recovery: token renewed")
                retryNow("new token")
            } catch (e: AmpacheException) {
                log.log("recovery: renewing failed (${e.message})")
                scheduleRetry()
            }
        }
    }

    private fun hasFailed() = player.playbackState == Player.STATE_IDLE && player.playerError != null

    private fun retryNow(why: String) {
        pending?.cancel()
        if (!hasFailed()) return
        log.log("recovery: prepare() at ${player.currentPosition / 1000} s ($why)")
        player.prepare()
    }
}
