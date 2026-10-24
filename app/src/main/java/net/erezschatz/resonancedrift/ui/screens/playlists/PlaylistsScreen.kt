package net.erezschatz.resonancedrift.ui.screens.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.data.entity.PlaylistEntity
import net.erezschatz.resonancedrift.ui.components.ListRow
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.countsLine

@Composable
fun PlaylistsScreen(viewModel: PlaylistsViewModel, onOpen: (PlaylistEntity) -> Unit) {
    val listing by viewModel.playlists.collectAsStateWithLifecycle()
    LazyColumn {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                ScreenHeader("Playlists")
                ListStatus(listing.refresh, listing.items.isEmpty(), emptyText = "The server has no playlists.")
            }
        }
        items(listing.items, key = { it.id }) { playlist ->
            ListRow(playlist.name, countsLine(albums = 0, songs = playlist.itemCount), onClick = { onOpen(playlist) })
        }
    }
}
