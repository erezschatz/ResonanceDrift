package net.erezschatz.resonancedrift.ui.screens.albums

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import net.erezschatz.resonancedrift.ui.components.Placeholder

@Composable
fun AlbumsScreen(viewModel: AlbumsViewModel) {
    Placeholder("Albums", "The album grid arrives in step 08. Until then:") {
        Button(onClick = { viewModel.playRandomAlbum() }) {
            Text("Play a random album")
        }
        viewModel.status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}
