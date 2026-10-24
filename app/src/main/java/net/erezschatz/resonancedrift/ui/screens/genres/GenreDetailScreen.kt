package net.erezschatz.resonancedrift.ui.screens.genres

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.ui.components.AlbumGrid
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.artistAndYear
import net.erezschatz.resonancedrift.ui.components.plural

@Composable
fun GenreDetailScreen(
    viewModel: GenreDetailViewModel,
    name: String,
    onOpenAlbum: (AlbumEntity) -> Unit,
) {
    AlbumGrid(
        albums = viewModel.albums,
        login = viewModel.login(),
        subtitle = { artistAndYear(it) },
        onOpen = onOpenAlbum,
    ) {
        Column {
            ScreenHeader(name, detail = if (viewModel.albums.isEmpty()) "" else plural(viewModel.albums.size, "album"))
            ListStatus(viewModel.refresh, viewModel.albums.isEmpty(), emptyText = "No albums in this genre on the server.")
        }
    }
}
