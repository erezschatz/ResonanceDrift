package net.erezschatz.resonancedrift.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.Listing
import net.erezschatz.resonancedrift.data.Refresh
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.SongEntity
import net.erezschatz.resonancedrift.ui.player.PlayerConnection

/** One album: its cached row and tracks at once, refreshed from the server behind them. */
class AlbumDetailViewModel(
    albumId: Long,
    private val session: Session,
    library: LibraryRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    val album: StateFlow<AlbumEntity?> = library.album(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), null)

    val songs: StateFlow<Listing<SongEntity>> = library.albumSongs(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), Listing(emptyList(), Refresh.Running))

    fun login() = session.currentLogin()

    /** The album as the queue, from track [index]. */
    fun playFrom(index: Int) = player.playSongs(songs.value.items, index, session.currentLogin())

    companion object {
        /** Keeps the refresh alive through a rotation instead of restarting it. */
        const val STOP_AFTER_MS = 5_000L

        fun factory(albumId: Long, session: Session, library: LibraryRepository, player: PlayerConnection): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AlbumDetailViewModel(albumId, session, library, player) }
            }
    }
}
