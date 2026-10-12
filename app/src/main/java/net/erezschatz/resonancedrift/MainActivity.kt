package net.erezschatz.resonancedrift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.erezschatz.resonancedrift.ui.theme.ResonanceDriftTheme

/**
 * The single Activity. It hosts the Compose UI and nothing else; playback
 * state comes from the service through PlayerConnection once that exists.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResonanceDriftTheme {
                // Placeholder until ui/nav lands. Proves the toolchain end to end:
                // Gradle → Kotlin → Compose → phone.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "ResonanceDrift",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
