package net.erezschatz.resonancedrift.ui.screens.dev

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

/** TEMPORARY (task 04): one album, Play, Pause, copy diagnostics. Deleted in task 07. */
@Composable
fun DevPlayerScreen(viewModel: DevPlayerViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current
    val connected by viewModel.player.connected.collectAsStateWithLifecycle()
    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()
    val currentTitle by viewModel.player.currentTitle.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Playback test",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = viewModel.albumLine,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { viewModel.playAlbum() },
                enabled = connected && viewModel.items.isNotEmpty(),
            ) {
                Text("Play album")
            }
            OutlinedButton(onClick = { viewModel.load() }) {
                Text("Another album")
            }
            Text(
                text = currentTitle?.let { "Now: $it" } ?: "Nothing playing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { viewModel.player.togglePlayPause() }, enabled = connected && currentTitle != null) {
                Text(if (isPlaying) "Pause" else "Resume")
            }
            OutlinedButton(onClick = {
                // Android 13+ confirms the copy itself; no toast of our own.
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("ResonanceDrift diagnostics", viewModel.diagnosticsReport()))
            }) {
                Text("Copy diagnostics")
            }
            OutlinedButton(onClick = onLogout) {
                Text("Log out")
            }
        }
    }
}
