package net.erezschatz.resonancedrift

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.SessionState
import net.erezschatz.resonancedrift.ui.screens.auth.AuthScreen
import net.erezschatz.resonancedrift.ui.screens.auth.AuthViewModel
import net.erezschatz.resonancedrift.ui.screens.dev.DevPlayerScreen
import net.erezschatz.resonancedrift.ui.screens.dev.DevPlayerViewModel
import net.erezschatz.resonancedrift.ui.theme.ResonanceDriftTheme

/**
 * The single Activity. It hosts the Compose UI and nothing else; playback
 * state comes from the service through PlayerConnection.
 */
class MainActivity : ComponentActivity() {

    /** The answer does not matter: playback works either way, only the notification differs. */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotificationsOnce()
        val session = graph.session
        setContent {
            ResonanceDriftTheme {
                val state by session.state.collectAsStateWithLifecycle()
                when (val current = state) {
                    // Credentials not read yet — a blank frame, not a flash of the auth screen.
                    SessionState.Unknown -> Box(Modifier.fillMaxSize())
                    is SessionState.LoggedOut -> AuthScreen(viewModel(factory = AuthViewModel.factory(session)))
                    // TEMPORARY until navigation (task 07).
                    is SessionState.LoggedIn -> DevPlayerScreen(
                        viewModel = viewModel(
                            factory = DevPlayerViewModel.factory(session, graph.ampache, graph.playerConnection),
                        ),
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
        graph.playerConnection.connect()
    }

    override fun onStop() {
        // Unbind while invisible; a playing service stays alive on its own (foreground).
        graph.playerConnection.disconnect()
        super.onStop()
    }

    private fun askForNotificationsOnce() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return
        lifecycleScope.launch {
            if (graph.settings.notificationPermissionAsked()) return@launch
            graph.settings.markNotificationPermissionAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
