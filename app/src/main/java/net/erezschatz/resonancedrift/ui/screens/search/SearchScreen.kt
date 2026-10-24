package net.erezschatz.resonancedrift.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.ui.components.AlbumGrid
import net.erezschatz.resonancedrift.ui.components.SongRow
import net.erezschatz.resonancedrift.ui.components.StatusText
import net.erezschatz.resonancedrift.ui.components.artistAndYear
import net.erezschatz.resonancedrift.ui.components.fullWidth
import net.erezschatz.resonancedrift.ui.components.joinKnown

/**
 * One box; albums first, then the matching songs (hidden when there are none).
 * The box stays put while the results scroll under it.
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel, onOpenAlbum: (AlbumEntity) -> Unit, onPlayed: () -> Unit) {
    val focus = LocalFocusManager.current
    val result = viewModel.result
    val albums = result?.albums.orEmpty()
    val songs = result?.songs.orEmpty()

    Column {
        OutlinedTextField(
            value = viewModel.query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            placeholder = { Text("Albums, artists, songs") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (viewModel.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            // The search is already running; the key only puts the keyboard away.
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        )
        if (viewModel.searching) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
        }
        AlbumGrid(
            albums = albums,
            login = viewModel.login(),
            subtitle = { artistAndYear(it) },
            onOpen = onOpenAlbum,
            footer = {
                result?.songsError?.let { error -> fullWidth { StatusText("Song results could not be loaded. ${error.userMessage}") } }
                if (songs.isNotEmpty()) {
                    fullWidth { SectionTitle("Songs") }
                    itemsIndexed(songs, key = { index, song -> "song-$index-${song.id}" }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, song ->
                        SongRow(
                            number = index + 1,
                            title = song.title,
                            detail = joinKnown(song.artistName, song.albumName),
                            durationSeconds = song.durationSeconds,
                            onClick = {
                                viewModel.play(song)
                                onPlayed()
                            },
                        )
                    }
                }
            },
        ) {
            Column {
                if (result != null) {
                    result.albumsError?.let { StatusText("Some album results could not be loaded. ${it.userMessage}") }
                    if (albums.isEmpty() && songs.isEmpty() && !viewModel.searching) {
                        StatusText("No results for \"${viewModel.query.trim()}\".")
                    }
                    if (albums.isNotEmpty()) SectionTitle("Albums")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}
