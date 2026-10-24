package net.erezschatz.resonancedrift.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.SearchResult
import net.erezschatz.resonancedrift.data.entity.SongEntity
import net.erezschatz.resonancedrift.ui.player.PlayerConnection

/**
 * The search box and its results. Typing is debounced; a newer query cancels
 * the search still running for an older one. Scoped to the Activity, not the
 * back-stack entry, so the query survives a trip to another tab (D33).
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val session: Session,
    private val library: LibraryRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    /** Null before the first search, and while the box is too short to search. */
    var result by mutableStateOf<SearchResult?>(null)
        private set

    var searching by mutableStateOf(false)
        private set

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries.debounce(DEBOUNCE_MS).collectLatest { text ->
                val trimmed = text.trim()
                if (trimmed.length < MIN_LENGTH) {
                    result = null
                    searching = false
                    return@collectLatest
                }
                searching = true
                result = library.search(trimmed)
                searching = false
            }
        }
    }

    fun login() = session.currentLogin()

    fun onQueryChange(text: String) {
        query = text
        queries.value = text
    }

    /** A song from the results plays on its own (task 10). */
    fun play(song: SongEntity) = player.playSongs(listOf(song), 0, session.currentLogin())

    companion object {
        /** Long enough to skip the keystrokes in between, short enough to feel live. */
        const val DEBOUNCE_MS = 350L

        /** One letter matches half the library; the server's answer would be noise. */
        const val MIN_LENGTH = 2

        fun factory(session: Session, library: LibraryRepository, player: PlayerConnection): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SearchViewModel(session, library, player) }
            }
    }
}
