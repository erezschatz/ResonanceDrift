package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AmpacheException

/**
 * `record_play` once per song start (task 13), fire and forget: a failure is
 * logged, never retried or shown. A song counts when it starts playing: at
 * its transition if the player is playing (one song running into the next),
 * else when play follows — a screen's Play sets the queue before it says play,
 * and a song only queued up paused is not a play.
 */
class Scrobbler(
    private val record: suspend (songId: Long) -> Unit,
    private val log: PlaybackLog,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var currentSongId: Long? = null
    private var playing = false

    /** The current song, until it has been counted. */
    private var pendingSongId: Long? = null

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val songId = mediaItem?.mediaId?.toLongOrNull()
        // A new queue that starts with the song already playing is the same play, not another.
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && songId != null && songId == currentSongId) return
        currentSongId = songId
        pendingSongId = songId
        if (playing) recordPending()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playing = isPlaying
        if (isPlaying) recordPending()
    }

    private fun recordPending() {
        val songId = pendingSongId ?: return
        pendingSongId = null
        scope.launch {
            try {
                record(songId)
            } catch (e: AmpacheException) {
                log.log("record_play song-$songId failed (${e.userMessage})")
            }
        }
    }

    /** The restored song was counted when it first started; resuming it is not a new play. */
    fun skipCurrent() {
        pendingSongId = null
    }
}
