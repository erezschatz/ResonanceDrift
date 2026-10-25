package net.erezschatz.resonancedrift.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private var recovery: NetworkRecovery? = null
    private var shuffle: AlbumShuffle? = null
    private var persistence: QueuePersistence? = null

    /** Main-thread scope for recovery's backoff timers; cancelled with the service. */
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        graph.playbackLog.startRun("run started · ${graph.appVersion} · ${graph.deviceDescription}")
        val player = PlayerFactory.create(this, graph.httpClient, graph.mediaCache, graph.songUriResolver, graph.playbackLog)
        player.addAnalyticsListener(PlaybackEvents(graph.playbackLog))
        recovery = NetworkRecovery(
            player,
            graph.session,
            graph.playbackLog,
            getSystemService(ConnectivityManager::class.java),
            scope,
        ).also { it.start() }
        shuffle = AlbumShuffle(
            PlayerQueueController(player),
            AppShuffleLibrary(graph.library, graph.shuffleHistory, graph.session),
            graph.playbackLog,
            scope,
            onStateChanged = { state, message ->
                mediaSession?.setSessionExtras(Commands.extras(state, message))
                persistence?.save()
            },
        ).also { player.addListener(it) }
        val scrobbler = Scrobbler(
            record = { songId -> graph.session.call { server, token -> graph.ampache.recordPlay(server, token, songId) } },
            graph.playbackLog,
            scope,
        ).also { player.addListener(it) }
        val persistence = QueuePersistence(
            player,
            graph.queueStateStore,
            graph.playbackLog,
            tickScope = scope,
            writeScope = graph.appScope,
            shuffleOn = { shuffle?.state == ShuffleState.ON },
        ).also { player.addListener(it) }
        this.persistence = persistence
        scope.launch { restoreQueue(persistence, scrobbler) }
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setSessionActivity(openAppIntent())
            .setSessionExtras(Commands.extras(ShuffleState.OFF, null))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        graph.playbackLog.log("service destroyed")
        persistence?.save()
        persistence = null
        recovery?.stop()
        recovery = null
        shuffle = null
        scope.cancel()
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /** Yesterday's queue, paused at its song and position (D8); shuffle carries on after it if it was on. */
    private suspend fun restoreQueue(persistence: QueuePersistence, scrobbler: Scrobbler) {
        val restored = persistence.restore { songIds ->
            // Logged out: nothing to stream with, so nothing to restore.
            val login = graph.session.awaitLogin() ?: return@restore emptyList()
            graph.library.songsForQueue(songIds).map { MediaItems.fromSong(it.toSong(), login) }
        } ?: return
        scrobbler.skipCurrent()
        if (restored.shuffle) shuffle?.continueFromCurrent()
    }

    /** Tapping the notification opens the app where it was. */
    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private inner class SessionCallback : MediaSession.Callback {

        /** Every controller (our UI) may send the shuffle commands. */
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            for (command in Commands.ALL) commands.add(command)
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands.build())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val shuffle = shuffle ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
            when (customCommand.customAction) {
                Commands.SHUFFLE_START.customAction -> shuffle.start(
                    if (args.containsKey(Commands.ARG_ALBUM_ID)) args.getLong(Commands.ARG_ALBUM_ID) else null,
                )
                Commands.SHUFFLE_CONTINUE.customAction -> shuffle.continueFromCurrent()
                Commands.SHUFFLE_STOP.customAction -> shuffle.stop()
                Commands.NEXT_ALBUM.customAction -> shuffle.nextAlbum()
                else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /**
         * Items arrive from the controller without their URI; put it back. A queue
         * set from outside the service (a screen's Play) turns album shuffle off —
         * shuffle's own appends go to the player directly, not through here.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            shuffle?.stop()
            return Futures.immediateFuture(mediaItems.map { MediaItems.withPlaybackUri(it) }.toMutableList())
        }
    }
}
