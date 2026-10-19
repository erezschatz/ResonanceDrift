package net.erezschatz.resonancedrift.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.ampache.SessionState

/**
 * Holds the three fields and the Connect action. On success it does nothing
 * further: [Session.state] flips to LoggedIn and MainActivity swaps screens.
 */
class AuthViewModel(private val session: Session) : ViewModel() {

    private val prefill = session.state.value as? SessionState.LoggedOut

    var serverUrl by mutableStateOf(prefill?.serverUrl.orEmpty())
    var username by mutableStateOf(prefill?.username.orEmpty())
    var password by mutableStateOf("")

    var connecting by mutableStateOf(false)
        private set

    /** Inline error under the form; null when there is nothing to show. */
    var error by mutableStateOf<String?>(null)
        private set

    val canConnect: Boolean
        get() = !connecting && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotEmpty()

    fun connect() {
        if (!canConnect) return
        connecting = true
        error = null
        viewModelScope.launch {
            try {
                session.login(serverUrl, username.trim(), password)
                // This ViewModel outlives the screen (it is Activity-scoped);
                // don't leave the password in memory for the next log out.
                password = ""
            } catch (e: AmpacheException) {
                error = e.message
            } finally {
                connecting = false
            }
        }
    }

    companion object {
        fun factory(session: Session): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(session) }
        }
    }
}
