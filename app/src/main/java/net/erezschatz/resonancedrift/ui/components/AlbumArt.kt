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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

/** Album art at [sizePx] (the request size), clipped to the card shape; an empty tile without an album. */
@Composable
fun AlbumArt(albumId: Long?, login: StoredLogin?, sizePx: Int, modifier: Modifier = Modifier) {
    val shaped = modifier
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.outline)
    if (albumId == null) {
        Box(shaped)
        return
    }
    AsyncImage(
        model = albumArtRequest(LocalContext.current, login, albumId, sizePx),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = shaped,
    )
}

/** Square art of a fixed size in dp, requested at that many pixels on this screen. */
@Composable
fun AlbumArtThumbnail(albumId: Long?, login: StoredLogin?, sizeDp: Int) {
    val sizePx = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
    AlbumArt(albumId, login, sizePx, Modifier.size(sizeDp.dp))
}

/** Request sizes shared across screens, so one cached image serves every card of that size. */
object ArtSizes {
    const val CARD_PX = 400
    const val HEADER_PX = 800
}
