package net.erezschatz.resonancedrift.ui.components

import android.content.Context
import coil3.request.ImageRequest
import net.erezschatz.resonancedrift.ampache.AmpacheUrls
import net.erezschatz.resonancedrift.ampache.StoredLogin

/**
 * The Coil request for an album's art. The URL carries the session token, so
 * Coil's default cache key (the URL) would miss after every re-handshake; the
 * memory and disk cache keys are the album id and size instead. No art is
 * stored in Room (docs/SPEC.md → Data).
 */
fun albumArtRequest(context: Context, login: StoredLogin?, albumId: Long, sizePx: Int): ImageRequest {
    val key = "album-art-$albumId-$sizePx"
    val url = login?.let { AmpacheUrls.art(it.serverUrl, it.token, albumId, sizePx) }
    return ImageRequest.Builder(context)
        .data(url)
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .build()
}
