package net.erezschatz.resonancedrift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.SessionState
import net.erezschatz.resonancedrift.ui.screens.auth.AuthScreen
import net.erezschatz.resonancedrift.ui.screens.auth.AuthViewModel
import net.erezschatz.resonancedrift.ui.theme.ResonanceDriftTheme

/**
 * The single Activity. It hosts the Compose UI and nothing else; playback
 * state comes from the service through PlayerConnection once that exists.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = graph.session
        setContent {
            ResonanceDriftTheme {
                val state by session.state.collectAsStateWithLifecycle()
                when (val current = state) {
                    // Credentials not read yet — a blank frame, not a flash of the auth screen.
                    SessionState.Unknown -> Box(Modifier.fillMaxSize())
                    is SessionState.LoggedOut -> AuthScreen(viewModel(factory = AuthViewModel.factory(session)))
                    is SessionState.LoggedIn -> LoggedInPlaceholder(
                        state = current,
                        onLogout = { lifecycleScope.launch { session.logout() } },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Foreground keep-alive (docs/AMPACHE.md); Session throttles and decides.
        lifecycleScope.launch { graph.session.pingIfStale() }
    }
}

/** Stands in for the app until navigation lands (task 07). Only exit: Log out. */
@Composable
private fun LoggedInPlaceholder(state: SessionState.LoggedIn, onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "ResonanceDrift",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${state.username} @ ${state.serverUrl}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onLogout) {
                Text("Log out")
            }
        }
    }
}
