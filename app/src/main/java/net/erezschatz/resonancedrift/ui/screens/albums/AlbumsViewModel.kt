package net.erezschatz.resonancedrift.ui.screens.albums

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.AlbumNames
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.Refresh
import net.erezschatz.resonancedrift.playback.MediaItems
import net.erezschatz.resonancedrift.ui.player.PlayerConnection

/**
 * TEMPORARY (step 07): the dev screen's "random album" carried over so the
 * phone can keep testing playback until step 08 puts the real grid here (D28).
 */
class AlbumsViewModel(
    private val session: Session,
    private val library: LibraryRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    var status by mutableStateOf<String?>(null)
        private set

    fun playRandomAlbum() {
        status = "Loading an album…"
        viewModelScope.launch {
            try {
                val album = library.randomAlbums(count = 1).firstOrNull()
                if (album == null) {
                    status = "The server returned no albums."
                    return@launch
                }
                val listing = library.albumSongs(album.id).first { it.refresh != Refresh.Running }
                val failed = listing.refresh as? Refresh.Failed
                if (failed != null && listing.items.isEmpty()) {
                    status = failed.error.userMessage
                    return@launch
                }
                val login = session.currentLogin()
                player.playQueue(listing.items.map { MediaItems.fromSong(it.toSong(), login) })
                status = "${AlbumNames.display(album.name, album.artistName)} — ${album.artistName}"
            } catch (e: AmpacheException) {
                status = e.userMessage
            }
        }
    }

    companion object {
        fun factory(session: Session, library: LibraryRepository, player: PlayerConnection): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AlbumsViewModel(session, library, player) }
            }
    }
}
