package net.erezschatz.resonancedrift.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import net.erezschatz.resonancedrift.AppGraph
import net.erezschatz.resonancedrift.ui.components.BottomNav
import net.erezschatz.resonancedrift.ui.components.MiniPlayer
import net.erezschatz.resonancedrift.ui.screens.albums.AlbumsScreen
import net.erezschatz.resonancedrift.ui.screens.albums.AlbumsViewModel
import net.erezschatz.resonancedrift.ui.screens.artists.ArtistsScreen
import net.erezschatz.resonancedrift.ui.screens.genres.GenresScreen
import net.erezschatz.resonancedrift.ui.screens.nowplaying.NowPlayingScreen
import net.erezschatz.resonancedrift.ui.screens.nowplaying.NowPlayingViewModel
import net.erezschatz.resonancedrift.ui.screens.playlists.PlaylistsScreen
import net.erezschatz.resonancedrift.ui.screens.search.SearchScreen

/**
 * The logged-in app: NavDisplay over the Navigator's back stack, with the
 * mini player and bottom bar around it. Both are hidden on Now Playing.
 */
@Composable
fun AppScaffold(graph: AppGraph, navigator: Navigator = viewModel()) {
    val player = graph.playerConnection
    val current by player.current.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val showChrome = navigator.current != Route.NowPlaying

    Scaffold(
        bottomBar = {
            if (showChrome) {
                Column {
                    current?.let { entry ->
                        MiniPlayer(
                            entry = entry,
                            isPlaying = isPlaying,
                            login = graph.session.currentLogin(),
                            onTogglePlay = { player.togglePlayPause() },
                            onOpen = { navigator.open(Route.NowPlaying) },
                        )
                    }
                    BottomNav(navigator.currentTab, onSelect = { navigator.selectTab(it) })
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = navigator.backStack,
            // Now Playing draws under the system bars itself; the tabs sit inside the scaffold's padding.
            modifier = if (showChrome) Modifier.padding(padding) else Modifier,
            onBack = { navigator.back() },
            entryProvider = { route -> NavEntry(route) { Screen(route, graph, navigator) } },
        )
    }
}

@Composable
private fun Screen(route: Route, graph: AppGraph, navigator: Navigator) {
    when (route) {
        Route.Albums -> AlbumsScreen(
            viewModel(factory = AlbumsViewModel.factory(graph.session, graph.library, graph.playerConnection)),
        )
        Route.Artists -> ArtistsScreen()
        Route.Genres -> GenresScreen()
        Route.Playlists -> PlaylistsScreen()
        Route.Search -> SearchScreen()
        Route.NowPlaying -> NowPlayingScreen(
            viewModel = viewModel(
                factory = NowPlayingViewModel.factory(
                    graph.session,
                    graph.playerConnection,
                    graph.playbackLog,
                    listOf(graph.appVersion, graph.deviceDescription),
                ),
            ),
            onClose = { navigator.back() },
        )
    }
}
