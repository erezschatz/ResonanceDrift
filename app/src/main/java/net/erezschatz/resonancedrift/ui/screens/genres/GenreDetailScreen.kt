package net.erezschatz.resonancedrift.ui.screens.genres

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.ui.components.AlbumGrid
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.StatusText
import net.erezschatz.resonancedrift.ui.components.artistAndYear
import net.erezschatz.resonancedrift.ui.components.plural

@Composable
fun GenreDetailScreen(
    viewModel: GenreDetailViewModel,
    name: String,
    onOpenAlbum: (AlbumEntity) -> Unit,
    onPlaying: () -> Unit,
) {
    AlbumGrid(
        albums = viewModel.albums,
        login = viewModel.login(),
        subtitle = { artistAndYear(it) },
        onOpen = onOpenAlbum,
    ) {
        Column {
            ScreenHeader(name, detail = if (viewModel.albums.isEmpty()) "" else plural(viewModel.albums.size, "album"))
            Button(
                onClick = { viewModel.playAll(onPlaying) },
                enabled = !viewModel.startingPlayback,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (viewModel.startingPlayback) "Loading songs…" else "Play all")
            }
            viewModel.playError?.let { StatusText(it) }
            ListStatus(viewModel.refresh, viewModel.albums.isEmpty(), emptyText = "No albums in this genre on the server.")
        }
    }
}
