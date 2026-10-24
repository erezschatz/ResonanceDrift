package net.erezschatz.resonancedrift.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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

/** Square album art; an empty tile when there is no album or no login. */
@Composable
fun AlbumArtThumbnail(albumId: Long?, login: StoredLogin?, sizeDp: Int) {
    val modifier = Modifier
        .size(sizeDp.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.outline)
    if (albumId == null) {
        Box(modifier)
        return
    }
    // Pixels for the request: density × dp, so the art is sharp without being oversized.
    val density = LocalContext.current.resources.displayMetrics.density
    AsyncImage(
        model = albumArtRequest(LocalContext.current, login, albumId, (sizeDp * density).toInt()),
        contentDescription = null,
        modifier = modifier,
    )
}
