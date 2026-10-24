package net.erezschatz.resonancedrift.ui.screens.genres

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
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.Refresh
import net.erezschatz.resonancedrift.data.entity.AlbumEntity

/** A genre's albums (API-only, D27), sorted by year then name. */
class GenreDetailViewModel(
    private val genreId: Long,
    private val session: Session,
    private val library: LibraryRepository,
) : ViewModel() {

    var albums by mutableStateOf<List<AlbumEntity>>(emptyList())
        private set

    var refresh by mutableStateOf<Refresh>(Refresh.Running)
        private set

    init {
        load()
    }

    fun login() = session.currentLogin()

    fun load() {
        refresh = Refresh.Running
        viewModelScope.launch {
            refresh = try {
                albums = library.genreAlbums(genreId)
                Refresh.Done
            } catch (e: AmpacheException) {
                Refresh.Failed(e)
            }
        }
    }

    companion object {
        fun factory(genreId: Long, session: Session, library: LibraryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GenreDetailViewModel(genreId, session, library) }
            }
    }
}
