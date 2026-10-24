package net.erezschatz.resonancedrift.ui.screens.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import net.erezschatz.resonancedrift.ui.components.SongRow
import net.erezschatz.resonancedrift.ui.components.joinKnown
import net.erezschatz.resonancedrift.ui.components.plural
import net.erezschatz.resonancedrift.ui.components.yearText

/**
 * Header (art, name, artist, year), Play and Shuffle, then the tracks.
 * Tapping a track plays the album from it and opens Now Playing.
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

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        current.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = if (artistId != null) {
                            Modifier.clickable { onOpenArtist(artistId, current.artistName) }
                        } else {
                            Modifier
                        },
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
                    // "Shuffle mode from this album" — wired in step 12.
                    OutlinedButton(onClick = {}, enabled = false) {
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
            )
        }
    }
}
