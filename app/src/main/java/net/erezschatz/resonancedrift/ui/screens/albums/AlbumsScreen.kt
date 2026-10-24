package net.erezschatz.resonancedrift.ui.screens.albums

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.erezschatz.resonancedrift.data.Refresh
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.ui.components.AlbumGrid
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.artistAndYear

@Composable
fun AlbumsScreen(viewModel: AlbumsViewModel, onOpenAlbum: (AlbumEntity) -> Unit) {
    AlbumGrid(
        albums = viewModel.albums,
        login = viewModel.login(),
        subtitle = { artistAndYear(it) },
        onOpen = onOpenAlbum,
    ) {
        Column {
            ScreenHeader("Albums") {
                IconButton(onClick = { viewModel.load() }, enabled = viewModel.refresh != Refresh.Running) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Load different albums")
                }
            }
            // Wired in step 12 (album shuffle).
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Shuffle all")
            }
            ListStatus(viewModel.refresh, viewModel.albums.isEmpty(), emptyText = "The server returned no albums.")
        }
    }
}
