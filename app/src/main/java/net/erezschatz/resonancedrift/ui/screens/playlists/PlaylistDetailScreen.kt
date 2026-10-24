package net.erezschatz.resonancedrift.ui.screens.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScreenHeader
import net.erezschatz.resonancedrift.ui.components.SongRow
import net.erezschatz.resonancedrift.ui.components.StatusText
import net.erezschatz.resonancedrift.ui.components.plural

/**
 * Header (name, song count, Play), then every song. Tapping a song plays the
 * playlist from it. Each row's ⋮ menu removes the song (not on smart playlists).
 */
@Composable
fun PlaylistDetailScreen(viewModel: PlaylistDetailViewModel, name: String, onPlayed: () -> Unit) {
    val listing by viewModel.songs.collectAsStateWithLifecycle()
    val songs = listing.items

    LazyColumn {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                ScreenHeader(name, detail = if (songs.isEmpty()) "" else plural(songs.size, "song"))
                Button(
                    onClick = {
                        viewModel.playFrom(0)
                        onPlayed()
                    },
                    enabled = songs.isNotEmpty(),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Play")
                }
                viewModel.removeError?.let { StatusText(it) }
                ListStatus(listing.refresh, songs.isEmpty(), emptyText = "This playlist is empty.")
            }
        }
        itemsIndexed(songs, key = { index, song -> "$index-${song.id}" }) { index, song ->
            SongRow(
                number = index + 1,
                title = song.title,
                detail = song.artistName,
                durationSeconds = song.durationSeconds,
                enabled = viewModel.removingIndex != index,
                onClick = {
                    viewModel.playFrom(index)
                    onPlayed()
                },
                trailing = {
                    if (viewModel.canRemove) {
                        RemoveMenu(enabled = viewModel.removingIndex == null, onRemove = { viewModel.remove(index, song) })
                    }
                },
            )
        }
    }
}

/** ⋮ → "Remove from playlist": two taps, so a stray tap never deletes. */
@Composable
private fun RemoveMenu(enabled: Boolean, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, enabled = enabled) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Song options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Remove from playlist") },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}
