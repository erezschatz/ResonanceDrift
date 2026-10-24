package net.erezschatz.resonancedrift.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.playback.PlaybackLog
import net.erezschatz.resonancedrift.ui.player.PlayerConnection
import java.time.ZonedDateTime

/** Now Playing's state is the player's; this adds the diagnostics report and log out (D34). */
class NowPlayingViewModel(
    private val session: Session,
    val player: PlayerConnection,
    private val playbackLog: PlaybackLog,
    private val reportHeader: List<String>,
) : ViewModel() {

    fun login() = session.currentLogin()

    fun diagnosticsReport(): String =
        playbackLog.report(
            listOf("ResonanceDrift diagnostics") + reportHeader + "copied ${ZonedDateTime.now()}" + player.describe(),
        )

    /** Log out lives in Now Playing's ⋮ menu, behind a confirmation (D34). */
    fun logout() {
        viewModelScope.launch { session.logout() }
    }

    companion object {
        fun factory(
            session: Session,
            player: PlayerConnection,
            playbackLog: PlaybackLog,
            reportHeader: List<String>,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { NowPlayingViewModel(session, player, playbackLog, reportHeader) }
            }
    }
}
