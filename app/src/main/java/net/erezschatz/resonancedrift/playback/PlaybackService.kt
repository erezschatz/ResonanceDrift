package net.erezschatz.resonancedrift.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import net.erezschatz.resonancedrift.MainActivity
import net.erezschatz.resonancedrift.graph

/**
 * Owns the player and the queue. The UI is only a MediaController client;
 * Media3 provides the foreground notification, lock-screen and Bluetooth
 * controls, audio focus and becoming-noisy from the session alone.
 *
 * Not overridden on purpose: onTaskRemoved. Media3's default keeps the service
 * running when the app is swiped from Recents mid-playback, and stops it when
 * paused — exactly the behaviour wanted.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlayerFactory.create(this, graph.httpClient, graph.mediaCache, graph.songUriResolver)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setSessionActivity(openAppIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /** Tapping the notification opens the app where it was. */
    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private class SessionCallback : MediaSession.Callback {
        /** Items arrive from the controller without their URI; put it back. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.map { MediaItems.withPlaybackUri(it) }.toMutableList())
    }
}
