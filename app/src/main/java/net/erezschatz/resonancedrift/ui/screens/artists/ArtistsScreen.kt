package net.erezschatz.resonancedrift.ui.screens.artists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.data.entity.ArtistEntity
import net.erezschatz.resonancedrift.ui.components.ListRow
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.countsLine

@Composable
fun ArtistsScreen(viewModel: ArtistsViewModel, onOpen: (ArtistEntity) -> Unit) {
    val listing by viewModel.artists.collectAsStateWithLifecycle()
    LazyColumn {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                ScreenHeader("Artists", detail = if (listing.items.isEmpty()) "" else "${listing.items.size}")
                ListStatus(listing.refresh, listing.items.isEmpty(), emptyText = "The server returned no artists.")
            }
        }
        items(listing.items, key = { it.id }) { artist ->
            ListRow(artist.name, countsLine(artist.albumCount, artist.songCount), onClick = { onOpen(artist) })
        }
    }
}
