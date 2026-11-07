package net.erezschatz.resonancedrift.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.ampache.AlbumNames
import net.erezschatz.resonancedrift.ui.components.AlbumArt
import net.erezschatz.resonancedrift.ui.components.ArtSizes
import net.erezschatz.resonancedrift.ui.components.ListStatus
import net.erezschatz.resonancedrift.ui.components.ScrollToPlayingRowOnce
import net.erezschatz.resonancedrift.ui.components.SongRow
import net.erezschatz.resonancedrift.ui.components.TappableText
import net.erezschatz.resonancedrift.ui.components.joinKnown
import net.erezschatz.resonancedrift.ui.components.playingRowIndex
import net.erezschatz.resonancedrift.ui.components.plural
import net.erezschatz.resonancedrift.ui.components.yearText

/**
 * Header (art, name, artist, year), Play and Shuffle, then the tracks.
 * Tapping a track plays the album from it and opens Now Playing. The track
 * that is playing is highlighted and centred when the screen opens (step 18).
 */
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onPlayed: () -> Unit,
    onOpenArtist: (artistId: Long, name: String) -> Unit,
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val listing by viewModel.songs.collectAsStateWithLifecycle()
    val songs = listing.items
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val playingRow = playingRowIndex(songs.map { it.id }, currentSongId)

    val listState = rememberLazyListState()
    // Item 0 is the header, so the row's list index is one more.
    ScrollToPlayingRowOnce(listState, if (playingRow < 0) -1 else playingRow + 1)
    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val current = album
                AlbumArt(current?.id, viewModel.login(), ArtSizes.HEADER_PX, Modifier.size(240.dp))
                if (current != null) {
                    Text(
                        AlbumNames.display(current.name, current.artistName),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    val artistId = current.artistId
                    TappableText(
                        text = current.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        onClick = artistId?.let { id -> { onOpenArtist(id, current.artistName) } },
                    )
                    Text(
                        joinKnown(if (songs.isEmpty()) "" else plural(songs.size, "song"), yearText(current.year)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.playFrom(0)
                            onPlayed()
                        },
                        enabled = songs.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Play")
                    }
                    // Album shuffle starting with this album (ReDrift's "Shuffle Mode").
                    OutlinedButton(onClick = {
                        viewModel.shuffleFromHere()
                        onPlayed()
                    }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Shuffle")
                    }
                }
                ListStatus(listing.refresh, songs.isEmpty(), emptyText = "This album has no songs on the server.")
            }
        }
        itemsIndexed(songs, key = { index, song -> "$index-${song.id}" }) { index, song ->
            SongRow(
                number = if (song.track > 0) song.track else index + 1,
                title = song.title,
                durationSeconds = song.durationSeconds,
                onClick = {
                    viewModel.playFrom(index)
                    onPlayed()
                },
                modifier = if (song.id == currentSongId) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
            )
        }
    }
}
