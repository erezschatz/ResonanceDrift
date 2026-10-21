package net.erezschatz.resonancedrift.playback

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import net.erezschatz.resonancedrift.ampache.AmpacheUrls
import net.erezschatz.resonancedrift.ampache.StoredLogin
import java.io.IOException

/**
 * Turns the token-free `rd://song/<id>` in every MediaItem into the real stream
 * URL, with whatever token the session holds *at the moment of the request*.
 * A re-handshake therefore never touches the playlist (this replaces ReDrift's
 * `token-refreshed` URL rewriting). Runs on ExoPlayer's loading thread.
 */
class SongUriResolver(private val currentLogin: () -> StoredLogin?) : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val url = streamUrl(dataSpec.uri.toString()) ?: return dataSpec
        // The cache key (song id, set on the MediaItem) travels with the DataSpec
        // unchanged, so the token in the URL never affects caching.
        return dataSpec.withUri(Uri.parse(url))
    }

    /**
     * The stream URL for one of our URIs, or null for any other URI (left
     * alone). Logged out is an I/O error: the load fails like any other.
     */
    fun streamUrl(uri: String): String? {
        val songId = songIdFrom(uri) ?: return null
        val login = currentLogin() ?: throw IOException("Not logged in; cannot stream song $songId")
        return AmpacheUrls.stream(login.serverUrl, login.token, songId)
    }

    companion object {
        private const val PREFIX = "rd://song/"

        fun uriFor(songId: Long): String = "$PREFIX$songId"

        fun songIdFrom(uri: String): Long? =
            if (uri.startsWith(PREFIX)) uri.removePrefix(PREFIX).toLongOrNull() else null
    }
}
