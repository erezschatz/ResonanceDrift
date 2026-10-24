package net.erezschatz.resonancedrift.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * The only way AlbumShuffle touches the player: count, position, the album an
 * item belongs to, append, replace. Ours, so the shuffle logic is tested
 * against a fake (docs/SPEC.md → Playback).
 */
interface QueueController {
    val itemCount: Int
    val currentIndex: Int

    /** The album of the item at [index], or null when it has none. */
    fun albumIdAt(index: Int): Long?

    /** Adds [items] after the last one; if playback had already ended, plays the first of them. */
    fun append(items: List<MediaItem>)

    /** Makes [items] the whole queue and plays from the first. */
    fun replace(items: List<MediaItem>)
}

/** [QueueController] over the service's player. Main thread, like every player call. */
class PlayerQueueController(private val player: Player) : QueueController {

    override val itemCount: Int get() = player.mediaItemCount

    override val currentIndex: Int get() = player.currentMediaItemIndex

    override fun albumIdAt(index: Int): Long? =
        if (index in 0 until player.mediaItemCount) MediaItems.linksOf(player.getMediaItemAt(index).mediaMetadata).albumId else null

    override fun append(items: List<MediaItem>) {
        val firstNew = player.mediaItemCount
        val ended = player.playbackState == Player.STATE_ENDED
        player.addMediaItems(items)
        // The fetch lost the race with the last song: start the new album instead of sitting at the end.
        if (ended) {
            player.seekTo(firstNew, 0L)
            player.play()
        }
    }

    override fun replace(items: List<MediaItem>) {
        player.setMediaItems(items)
        player.prepare()
        player.play()
    }
}
