package net.erezschatz.resonancedrift.ui.screens.artists

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.ui.components.AlbumGrid
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.plural
import net.erezschatz.resonancedrift.ui.components.yearText

/** [name] comes with the route, so the header shows before the albums arrive. */
@Composable
fun ArtistDetailScreen(viewModel: ArtistDetailViewModel, name: String, onOpenAlbum: (AlbumEntity) -> Unit) {
    val listing by viewModel.albums.collectAsStateWithLifecycle()
    AlbumGrid(
        albums = listing.items,
        login = viewModel.login(),
        subtitle = { yearText(it.year) },
        onOpen = onOpenAlbum,
    ) {
        Column {
            ScreenHeader(name, detail = if (listing.items.isEmpty()) "" else plural(listing.items.size, "album"))
            ListStatus(listing.refresh, listing.items.isEmpty(), emptyText = "No albums for this artist on the server.")
        }
    }
}
