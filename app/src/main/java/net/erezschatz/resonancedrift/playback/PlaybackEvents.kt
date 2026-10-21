package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException
import java.util.Locale

/**
 * Translates ExoPlayer's events into [PlaybackLog] lines. Chosen to tell the
 * causes of an audible hiccup apart:
 *  - `state BUFFERING` while playing → the data ran out (network/server);
 *  - `audio underrun` → data was there but the audio track starved (CPU);
 *  - `suppressed` / `playWhenReady … reason` → audio focus, noisy, the user.
 * Every line carries the position and how much is buffered ahead.
 */
class PlaybackEvents(private val log: PlaybackLog) : AnalyticsListener {

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) =
        log.log("state ${stateName(state)} ${where(eventTime)}")

    override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) =
        log.log("${if (isPlaying) "playing" else "not playing"} ${where(eventTime)}")

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) =
        log.log("playWhenReady $playWhenReady (${playWhenReadyReason(reason)})")

    override fun onPlaybackSuppressionReasonChanged(eventTime: AnalyticsListener.EventTime, playbackSuppressionReason: Int) =
        log.log("suppressed: ${suppressionName(playbackSuppressionReason)} ${where(eventTime)}")

    override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) {
        val title = mediaItem?.mediaMetadata?.title ?: "?"
        log.log("→ item ${eventTime.currentWindowIndex + 1} song ${mediaItem?.mediaId} \"$title\" (${transitionReason(reason)})")
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) =
        log.log("ERROR ${error.errorCodeName} ${where(eventTime)}: ${causeChain(error)}")

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        retryCount: Int,
    ) = log.log("load start ${loadEventInfo.dataSpec.key}${if (retryCount > 0) " (retry $retryCount)" else ""}")

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) = log.log("load done ${loadEventInfo.dataSpec.key}: ${loadEventInfo.bytesLoaded / 1024} KB in ${loadEventInfo.loadDurationMs} ms")

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean,
    ) = log.log("load error ${loadEventInfo.dataSpec.key} ${where(eventTime)}: ${causeChain(error)}")

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long,
    ) = log.log("audio underrun ${where(eventTime)}: track buffer $bufferSizeMs ms, last fed $elapsedSinceLastFeedMs ms ago")

    private fun where(eventTime: AnalyticsListener.EventTime): String =
        "@${seconds(eventTime.currentPlaybackPositionMs)} buf +${seconds(eventTime.totalBufferedDurationMs)}"

    private fun seconds(ms: Long) = String.format(Locale.ROOT, "%.1fs", ms / 1000.0)

    companion object {
        fun causeChain(error: Throwable): String {
            val parts = mutableListOf<String>()
            var cause: Throwable? = error
            while (cause != null && parts.size < 4) {
                parts.add("${cause.javaClass.simpleName}: ${cause.message}")
                cause = cause.cause
            }
            return parts.joinToString(" ← ")
        }

        fun stateName(state: Int) = when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "?$state"
        }

        private fun playWhenReadyReason(reason: Int) = when (reason) {
            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "user"
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "audio focus lost"
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "headphones disconnected"
            Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "remote"
            Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "end of item"
            Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> "suppressed too long"
            else -> "reason $reason"
        }

        private fun suppressionName(reason: Int) = when (reason) {
            Player.PLAYBACK_SUPPRESSION_REASON_NONE -> "none"
            Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS -> "transient audio focus loss"
            Player.PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_OUTPUT -> "unsuitable audio output"
            else -> "reason $reason"
        }

        private fun transitionReason(reason: Int) = when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "auto"
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "seek/skip"
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "new queue"
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "repeat"
            else -> "reason $reason"
        }
    }
}
