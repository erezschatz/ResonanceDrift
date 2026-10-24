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
import net.erezschatz.resonancedrift.ui.player.PlayerConnection

/** A genre's albums (API-only, D27), sorted by year then name; Play all plays every song in it. */
class GenreDetailViewModel(
    private val genreId: Long,
    private val session: Session,
    private val library: LibraryRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    var albums by mutableStateOf<List<AlbumEntity>>(emptyList())
        private set

    var refresh by mutableStateOf<Refresh>(Refresh.Running)
        private set

    /** Set while Play all fetches the songs; the button waits. */
    var startingPlayback by mutableStateOf(false)
        private set

    /** Play all's failure, shown under the button. */
    var playError by mutableStateOf<String?>(null)
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

    /** Fetches the genre's songs and plays them; [onPlaying] runs once they are queued. */
    fun playAll(onPlaying: () -> Unit) {
        if (startingPlayback) return
        startingPlayback = true
        playError = null
        viewModelScope.launch {
            try {
                val songs = library.genreSongs(genreId)
                if (songs.isEmpty()) {
                    playError = "This genre has no songs on the server."
                } else {
                    player.playSongs(songs, 0, session.currentLogin())
                    onPlaying()
                }
            } catch (e: AmpacheException) {
                playError = e.userMessage
            } finally {
                startingPlayback = false
            }
        }
    }

    companion object {
        fun factory(genreId: Long, session: Session, library: LibraryRepository, player: PlayerConnection): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GenreDetailViewModel(genreId, session, library, player) }
            }
    }
}
