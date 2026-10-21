package net.erezschatz.resonancedrift.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import net.erezschatz.resonancedrift.ampache.AlbumNames
import net.erezschatz.resonancedrift.ampache.AmpacheUrls
import net.erezschatz.resonancedrift.ampache.StoredLogin
import net.erezschatz.resonancedrift.ampache.model.Song

/**
 * Song ↔ MediaItem. The metadata set here is what the notification, the lock
 * screen and Bluetooth displays show; Media3 publishes it, we only fill it in.
 */
object MediaItems {

    /** Art for the notification and lock screen; ReDrift used 512px for the media session. */
    private const val ART_SIZE_PX = 512

    /**
     * [login] supplies the art URL's token. Unlike the stream URL, the art URL
     * is fixed when the item is built; a stale token there only costs the
     * notification its picture (task 13 rebuilds items on queue restore).
     */
    fun fromSong(song: Song, login: StoredLogin?): MediaItem {
        val artist = song.artist?.name
        val albumArtist = song.albumartist?.name ?: artist
        val albumName = song.album?.name?.let { AlbumNames.display(it, albumArtist) }
        val albumId = song.album?.id

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(artist)
            .setAlbumTitle(albumName)
            .setAlbumArtist(albumArtist)
            .setTrackNumber(song.track.takeIf { it > 0 })
            .setReleaseYear(song.year.takeIf { it > 0 })
            .setDurationMs(song.time.takeIf { it > 0 }?.let { it * 1000L })
            .setArtworkUri(
                if (login != null && albumId != null) {
                    Uri.parse(AmpacheUrls.art(login.serverUrl, login.token, albumId, ART_SIZE_PX))
                } else {
                    null
                },
            )
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setMediaMetadata(metadata)
            .let { withPlaybackUri(it, song.id) }
            .build()
    }

    /**
     * Media3 strips a MediaItem's URI when a controller hands it to the session,
     * so the service rebuilds it from the media id (MediaSession.Callback.onAddMediaItems).
     */
    fun withPlaybackUri(item: MediaItem): MediaItem {
        val songId = item.mediaId.toLongOrNull() ?: return item
        return withPlaybackUri(item.buildUpon(), songId).build()
    }

    private fun withPlaybackUri(builder: MediaItem.Builder, songId: Long): MediaItem.Builder =
        builder
            .setUri(SongUriResolver.uriFor(songId))
            // Cache by song, not by URL: the URL carries a token that changes per session.
            .setCustomCacheKey("song-$songId")
}
