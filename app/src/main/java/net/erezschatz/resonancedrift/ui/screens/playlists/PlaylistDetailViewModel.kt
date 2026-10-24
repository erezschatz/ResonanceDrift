package net.erezschatz.resonancedrift.ui.screens.playlists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.Listing
import net.erezschatz.resonancedrift.data.Refresh
import net.erezschatz.resonancedrift.data.entity.SongEntity
import net.erezschatz.resonancedrift.ui.player.PlayerConnection

/**
 * One playlist's songs, cache-first. Removal goes to the server first; the row
 * is disabled meanwhile and leaves the list when the cache is updated (D31).
 */
class PlaylistDetailViewModel(
    private val playlistId: String,
    private val session: Session,
    private val library: LibraryRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    val songs: StateFlow<Listing<SongEntity>> = library.playlistSongs(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Listing(emptyList(), Refresh.Running))

    /** Smart playlists are computed by the server; it does not let songs be removed from them. */
    val canRemove: Boolean = !playlistId.startsWith(SMART_PREFIX)

    /** The row being removed, by index; one removal at a time. */
    var removingIndex by mutableStateOf<Int?>(null)
        private set

    var removeError by mutableStateOf<String?>(null)
        private set

    fun playFrom(index: Int) = player.playSongs(songs.value.items, index, session.currentLogin())

    fun remove(index: Int, song: SongEntity) {
        if (removingIndex != null) return
        removingIndex = index
        removeError = null
        viewModelScope.launch {
            try {
                library.removeFromPlaylist(playlistId, index, song.id)
            } catch (e: AmpacheException) {
                removeError = "Could not remove \"${song.title}\": ${e.userMessage}"
            } finally {
                removingIndex = null
            }
        }
    }

    companion object {
        private const val SMART_PREFIX = "smart_"

        fun factory(playlistId: String, session: Session, library: LibraryRepository, player: PlayerConnection): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PlaylistDetailViewModel(playlistId, session, library, player) }
            }
    }
}
