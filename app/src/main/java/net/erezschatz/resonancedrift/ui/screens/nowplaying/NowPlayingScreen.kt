package net.erezschatz.resonancedrift.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.ui.components.AlbumArt
import net.erezschatz.resonancedrift.ui.components.ArtSizes
import net.erezschatz.resonancedrift.ui.components.PlayPauseButton
import net.erezschatz.resonancedrift.ui.components.TappableText
import net.erezschatz.resonancedrift.ui.components.formatDuration
import net.erezschatz.resonancedrift.ui.components.genreLine
import net.erezschatz.resonancedrift.ui.components.joinKnown
import net.erezschatz.resonancedrift.ui.components.yearText
import net.erezschatz.resonancedrift.ui.player.Position
import net.erezschatz.resonancedrift.ui.player.QueueEntry

/** Where Now Playing's links go; each closes Now Playing first. */
class NowPlayingLinks(
    val onClose: () -> Unit,
    val onOpenArtist: (artistId: Long, name: String) -> Unit,
    val onOpenAlbum: (albumId: Long) -> Unit,
    val onOpenGenre: (genreId: Long, name: String) -> Unit,
)

/**
 * Full screen, no bars. Art; title; artist (→ Artist detail); album · year
 * (→ Album detail); genre line (→ Genre detail, hidden when none); seek bar;
 * shuffle / previous / play-pause / next / next album; the queue panel; and
 * the ⋮ menu with Copy diagnostics and Log out.
 */
@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel, links: NowPlayingLinks) {
    val player = viewModel.player
    val entry by player.current.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val isWaiting by player.isWaiting.collectAsStateWithLifecycle()
    val position by player.position.collectAsStateWithLifecycle()
    val queue by player.queue.collectAsStateWithLifecycle()
    val currentIndex by player.currentIndex.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        TopRow(viewModel, links.onClose, onShowQueue = { showQueue = true }, queueEnabled = queue.isNotEmpty())
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val current = entry
            if (current == null) {
                Spacer(Modifier.size(48.dp))
                Text("Nothing playing", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            AlbumArt(
                current.albumId,
                viewModel.login(),
                ArtSizes.HEADER_PX,
                Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f).padding(vertical = 8.dp),
            )
            SongLines(current, links)
            SeekBar(position, onSeek = { player.seekTo(it) })
            Controls(
                isPlaying = isPlaying,
                isWaiting = isWaiting,
                onPrevious = { player.previous() },
                onToggle = { player.togglePlayPause() },
                onNext = { player.next() },
            )
        }
    }

    if (showQueue) {
        QueuePanel(
            queue = queue,
            currentIndex = currentIndex,
            onJump = { player.jumpTo(it) },
            onRemove = { player.removeFromQueue(it) },
            onDismiss = { showQueue = false },
        )
    }
}

@Composable
private fun TopRow(viewModel: NowPlayingViewModel, onClose: () -> Unit, onShowQueue: () -> Unit, queueEnabled: Boolean) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onShowQueue, enabled = queueEnabled) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Copy diagnostics") },
                    onClick = {
                        menuOpen = false
                        // Android 13+ shows its own "copied" confirmation; no toast of ours on top.
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("ResonanceDrift diagnostics", viewModel.diagnosticsReport()))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Log out") },
                    onClick = {
                        menuOpen = false
                        confirmLogout = true
                    },
                )
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("The server and username stay filled in; you will need the password to log in again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            },
        )
    }
}

/** Title, then the three link lines. A line without an id is plain text. */
@Composable
private fun SongLines(entry: QueueEntry, links: NowPlayingLinks) {
    Text(entry.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    val artistId = entry.artistId
    TappableText(
        text = entry.artist,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        onClick = artistId?.let { id -> { links.onOpenArtist(id, entry.artist) } },
    )
    val albumId = entry.albumId
    TappableText(
        text = joinKnown(entry.album, yearText(entry.year)),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        onClick = albumId?.let { id -> { links.onOpenAlbum(id) } },
    )
    val firstGenre = entry.genres.firstOrNull()
    if (firstGenre != null) {
        // Two genres may show; a tap goes to the first (ReDrift TODO 2a: no picker).
        TappableText(
            text = genreLine(entry.genres),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            onClick = { links.onOpenGenre(firstGenre.id, firstGenre.name) },
        )
    }
}

/** Follows the player; while dragged it follows the finger and seeks on release. */
@Composable
private fun SeekBar(position: Position, onSeek: (Long) -> Unit) {
    var dragMs by remember { mutableStateOf<Float?>(null) }
    val duration = position.durationMs.toFloat()
    val shownMs = dragMs ?: position.positionMs.toFloat()

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Slider(
            value = if (duration > 0) shownMs.coerceIn(0f, duration) else 0f,
            onValueChange = { dragMs = it },
            onValueChangeFinished = {
                dragMs?.let { onSeek(it.toLong()) }
                dragMs = null
            },
            valueRange = 0f..(if (duration > 0) duration else 1f),
            enabled = duration > 0,
        )
        Row {
            Text(clock(shownMs.toLong()), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(clock(position.durationMs), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "0:00" for a zero or unknown time, so the line never jumps when the duration arrives. */
private fun clock(ms: Long): String = formatDuration((ms / 1000).toInt()).ifEmpty { "0:00" }

@Composable
private fun Controls(
    isPlaying: Boolean,
    isWaiting: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Album shuffle and Next album are wired in step 12.
        IconButton(onClick = {}, enabled = false) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Album shuffle")
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
        }
        PlayPauseButton(isPlaying, isWaiting, onToggle, sizeDp = 56)
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = {}, enabled = false) {
            Icon(Icons.Filled.Album, contentDescription = "Next album")
        }
    }
}
