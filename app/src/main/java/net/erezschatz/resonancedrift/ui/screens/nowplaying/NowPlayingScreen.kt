package net.erezschatz.resonancedrift.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.erezschatz.resonancedrift.ui.components.AlbumArtThumbnail

/**
 * A full-screen route (no bars). TEMPORARY layout until step 11: art, title,
 * artist · album, prev / play-pause / next, copy diagnostics, log out.
 */
@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val entry by viewModel.player.current.collectAsStateWithLifecycle()
    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            val current = entry
            if (current == null) {
                Text("Nothing playing", style = MaterialTheme.typography.bodyLarge)
            } else {
                AlbumArtThumbnail(current.albumId, viewModel.login(), sizeDp = 280)
                Text(current.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    "${current.artist} · ${current.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.player.previous() }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                    }
                    IconButton(onClick = { viewModel.player.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                        )
                    }
                    IconButton(onClick = { viewModel.player.next() }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                    }
                }
            }
            OutlinedButton(onClick = {
                // Android 13+ confirms the copy itself; no toast of our own.
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("ResonanceDrift diagnostics", viewModel.diagnosticsReport()))
            }) {
                Text("Copy diagnostics")
            }
            OutlinedButton(onClick = { viewModel.logout() }) {
                Text("Log out")
            }
        }
    }
}
