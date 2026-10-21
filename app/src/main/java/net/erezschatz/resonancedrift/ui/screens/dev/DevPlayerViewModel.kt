package net.erezschatz.resonancedrift.ui.screens.dev

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import net.erezschatz.resonancedrift.ampache.AlbumNames
import net.erezschatz.resonancedrift.ampache.AmpacheClient
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.playback.MediaItems
import net.erezschatz.resonancedrift.playback.PlaybackLog
import net.erezschatz.resonancedrift.ui.player.PlayerConnection

/**
 * TEMPORARY (task 04): the first album the server returns, ready to play.
 * Exists only to prove playback through a locked screen; deleted in task 07.
 */
class DevPlayerViewModel(
    private val session: Session,
    private val ampache: AmpacheClient,
    val player: PlayerConnection,
    private val playbackLog: PlaybackLog,
    private val reportHeader: List<String>,
) : ViewModel() {

    var albumLine by mutableStateOf("Loading an album…")
        private set

    var items by mutableStateOf<List<MediaItem>>(emptyList())
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val album = session.call { server, token -> ampache.albums(server, token, offset = 0, limit = 1) }
                    .firstOrNull()
                if (album == null) {
                    albumLine = "The server returned no albums."
                    return@launch
                }
                val songs = session.call { server, token -> ampache.albumSongs(server, token, album.id) }
                val login = session.currentLogin()
                items = songs.map { MediaItems.fromSong(it, login) }
                val artist = album.artist?.name
                albumLine = "${AlbumNames.display(album.name, artist)} — ${artist.orEmpty()} · ${songs.size} tracks"
            } catch (e: AmpacheException) {
                albumLine = e.userMessage
            }
        }
    }

    fun playAlbum() = player.playQueue(items)

    /** Moves to Now Playing in task 11. */
    fun diagnosticsReport(): String =
        playbackLog.report(
            listOf("ResonanceDrift diagnostics") + reportHeader + "copied ${ZonedDateTime.now()}" + player.describe(),
        )

    companion object {
        fun factory(
            session: Session,
            ampache: AmpacheClient,
            player: PlayerConnection,
            playbackLog: PlaybackLog,
            reportHeader: List<String>,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DevPlayerViewModel(session, ampache, player, playbackLog, reportHeader) }
            }
    }
}
