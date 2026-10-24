package net.erezschatz.resonancedrift.ui.screens.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.erezschatz.resonancedrift.data.LibraryRepository
import net.erezschatz.resonancedrift.data.Listing
import net.erezschatz.resonancedrift.data.Refresh
import net.erezschatz.resonancedrift.data.entity.ArtistEntity

/** Every artist, cache-first (D27). */
class ArtistsViewModel(library: LibraryRepository) : ViewModel() {

    val artists: StateFlow<Listing<ArtistEntity>> = library.artists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Listing(emptyList(), Refresh.Running))

    companion object {
        fun factory(library: LibraryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ArtistsViewModel(library) }
            }
    }
}
