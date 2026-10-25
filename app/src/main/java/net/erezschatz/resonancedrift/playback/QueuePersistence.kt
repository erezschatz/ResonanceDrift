package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.erezschatz.resonancedrift.data.QueueState
import net.erezschatz.resonancedrift.data.QueueStateStore
import java.util.Locale

/**
 * Saves the queue — song ids, index, position, shuffle flag — and restores it,
 * paused, when the service is created (task 13, D8). Saves on every transition,
 * queue change, seek and stop of playback, and every [SAVE_EVERY_MS] while
 * playing, so a force-stop loses at most that much position.
 *
 * Player calls on the main thread ([tickScope] is the service's). Writes go
 * through [writeScope], which outlives the service, so the save made in
 * onDestroy still lands; [writeLock] keeps them in order.
 */
class QueuePersistence(
    private val player: Player,
    private val store: QueueStateStore,
    private val log: PlaybackLog,
    private val tickScope: CoroutineScope,
    private val writeScope: CoroutineScope,
    private val shuffleOn: () -> Boolean,
) : Player.Listener {

    /** No saves until [restore] is done: the empty new player would overwrite the saved queue. */
    private var restoring = true

    private var ticker: Job? = null
    private val writeLock = Mutex()

    /**
     * Rebuilds the saved queue with [load] (song ids → items, missing ids left
     * out) at the saved song and position, prepared and paused. Does nothing if
     * a queue was set while loading. Returns the saved state when restored.
     */
    suspend fun restore(load: suspend (List<Long>) -> List<MediaItem>): QueueState? {
        try {
            val saved = store.load() ?: return null
            if (saved.songIds.isEmpty()) return null
            val items = load(saved.songIds)
            if (player.mediaItemCount > 0) {
                log.log("queue not restored: a new queue was set meanwhile")
                // Its saves were held back while loading; this one replaces the old queue.
                restoring = false
                save()
                return null
            }
            val start = startOf(saved, items.mapNotNull { it.mediaId.toLongOrNull() })
            if (start == null) {
                log.log("queue not restored: none of its ${saved.songIds.size} songs could be loaded")
                return null
            }
            player.setMediaItems(items, start.index, start.positionMs)
            // Prepared so one tap plays; playWhenReady stays false (D8: never auto-resume).
            player.prepare()
            val missing = saved.songIds.size - items.size
            log.log(
                "queue restored: ${items.size} songs, item ${start.index + 1} at ${seconds(start.positionMs)}" +
                    (if (saved.shuffle) ", shuffle on" else "") +
                    (if (missing > 0) ", $missing not found" else ""),
            )
            return saved
        } finally {
            restoring = false
        }
    }

    /** Writes the queue as the player has it now; an empty queue clears the saved one. */
    fun save() {
        if (restoring) return
        val state = QueueState(
            songIds = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() },
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0),
            shuffle = shuffleOn(),
        )
        writeScope.launch {
            writeLock.withLock {
                if (state.songIds.isEmpty()) store.clear() else store.save(state)
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = save()

    override fun onTimelineChanged(timeline: Timeline, reason: Int) = save()

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) save()
    }

    /** Stopping (pause, focus loss, buffering) saves; playing saves every [SAVE_EVERY_MS]. */
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (ticker == null) {
                ticker = tickScope.launch {
                    while (isActive) {
                        delay(SAVE_EVERY_MS)
                        save()
                    }
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
            save()
        }
    }

    /** Where a restored queue starts: an index into the songs that loaded, and a position. */
    data class Start(val index: Int, val positionMs: Long)

    companion object {
        /** The task's "every ~10 s": the most position a force-stop can lose. */
        const val SAVE_EVERY_MS = 10_000L

        /**
         * [loaded] is [saved]'s song ids in order with the missing ones left out.
         * The saved song at its saved position if it loaded; otherwise the start
         * of the next one that did (the last, if none after it did); null if none loaded.
         */
        fun startOf(saved: QueueState, loaded: List<Long>): Start? {
            if (loaded.isEmpty()) return null
            val loadedIds = loaded.toSet()
            val before = saved.songIds.take(saved.index).count { it in loadedIds }
            val currentLoaded = saved.songIds.getOrNull(saved.index)?.let { it in loadedIds } ?: false
            return if (currentLoaded) {
                Start(before, saved.positionMs)
            } else {
                Start(before.coerceAtMost(loaded.size - 1), 0L)
            }
        }

        private fun seconds(ms: Long) = String.format(Locale.ROOT, "%.1fs", ms / 1000.0)
    }
}
