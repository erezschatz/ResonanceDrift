package net.erezschatz.resonancedrift.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.erezschatz.resonancedrift.ampache.AlbumNames
import net.erezschatz.resonancedrift.ampache.StoredLogin
import net.erezschatz.resonancedrift.data.entity.AlbumEntity

/**
 * The album grid used by Albums, Artist detail and Genre detail. [header]
 * spans the full width above the cards (title, buttons, status line).
 * Columns adapt to the width, ~150 dp each, as ReDrift's CSS grid did.
 */
@Composable
fun AlbumGrid(
    albums: List<AlbumEntity>,
    login: StoredLogin?,
    subtitle: (AlbumEntity) -> String,
    onOpen: (AlbumEntity) -> Unit,
    header: @Composable () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = rememberLazyGridState(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        fullWidth { header() }
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, login, subtitle(album), onClick = { onOpen(album) })
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
fun AlbumCard(album: AlbumEntity, login: StoredLogin?, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        AlbumArt(album.id, login, ArtSizes.CARD_PX, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            text = AlbumNames.display(album.name, album.artistName),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Artist · 2003", or just the part that is known. */
fun artistAndYear(album: AlbumEntity): String = joinKnown(album.artistName, yearText(album.year))

fun yearText(year: Int): String = if (year > 0) year.toString() else ""

/** Joins the non-empty parts with " · ". */
fun joinKnown(vararg parts: String): String = parts.filter { it.isNotEmpty() }.joinToString(" · ")
