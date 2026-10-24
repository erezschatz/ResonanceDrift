package net.erezschatz.resonancedrift.ui.screens.artists

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

/** One artist's albums, cache-first, newest first. */
class ArtistDetailViewModel(
    artistId: Long,
    private val session: Session,
    library: LibraryRepository,
) : ViewModel() {

    val albums: StateFlow<Listing<AlbumEntity>> = library.artistAlbums(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Listing(emptyList(), Refresh.Running))

    fun login() = session.currentLogin()

    companion object {
        fun factory(artistId: Long, session: Session, library: LibraryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ArtistDetailViewModel(artistId, session, library) }
            }
    }
}
