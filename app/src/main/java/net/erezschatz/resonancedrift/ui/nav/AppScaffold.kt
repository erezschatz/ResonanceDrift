package net.erezschatz.resonancedrift.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import net.erezschatz.resonancedrift.AppGraph
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.ui.components.BottomNav
import net.erezschatz.resonancedrift.ui.components.MiniPlayer
import net.erezschatz.resonancedrift.ui.screens.albums.AlbumDetailScreen
import net.erezschatz.resonancedrift.ui.screens.albums.AlbumDetailViewModel
import net.erezschatz.resonancedrift.ui.screens.albums.AlbumsScreen
import net.erezschatz.resonancedrift.ui.screens.albums.AlbumsViewModel
import net.erezschatz.resonancedrift.ui.screens.artists.ArtistDetailScreen
import net.erezschatz.resonancedrift.ui.screens.artists.ArtistDetailViewModel
import net.erezschatz.resonancedrift.ui.screens.artists.ArtistsScreen
import net.erezschatz.resonancedrift.ui.screens.artists.ArtistsViewModel
import net.erezschatz.resonancedrift.ui.screens.genres.GenreDetailScreen
import net.erezschatz.resonancedrift.ui.screens.genres.GenreDetailViewModel
import net.erezschatz.resonancedrift.ui.screens.genres.GenresScreen
import net.erezschatz.resonancedrift.ui.screens.genres.GenresViewModel
import net.erezschatz.resonancedrift.ui.screens.nowplaying.NowPlayingScreen
import net.erezschatz.resonancedrift.ui.screens.nowplaying.NowPlayingViewModel
import net.erezschatz.resonancedrift.ui.screens.playlists.PlaylistDetailScreen
import net.erezschatz.resonancedrift.ui.screens.playlists.PlaylistDetailViewModel
import net.erezschatz.resonancedrift.ui.screens.playlists.PlaylistsScreen
import net.erezschatz.resonancedrift.ui.screens.playlists.PlaylistsViewModel
import net.erezschatz.resonancedrift.ui.screens.search.SearchScreen
import net.erezschatz.resonancedrift.ui.screens.search.SearchViewModel

/**
 * The logged-in app: NavDisplay over the Navigator's back stack, with the
 * mini player and bottom bar around it. Both are hidden on Now Playing.
 */
@Composable
fun AppScaffold(graph: AppGraph, navigator: Navigator = viewModel()) {
    val player = graph.playerConnection
    val current by player.current.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val isWaiting by player.isWaiting.collectAsStateWithLifecycle()
    val showChrome = navigator.current != Route.NowPlaying
    // The Activity's ViewModel store, for the one screen whose state outlives its entry (Search, D33).
    val activityStore = checkNotNull(LocalViewModelStoreOwner.current)

    Scaffold(
        bottomBar = {
            if (showChrome) {
                Column {
                    current?.let { entry ->
                        MiniPlayer(
                            entry = entry,
                            isPlaying = isPlaying,
                            isWaiting = isWaiting,
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
            entryDecorators = listOf(
                // Keeps each entry's remembered state (scroll position) while it is on the stack.
                rememberSaveableStateHolderNavEntryDecorator(),
                // A ViewModel per entry: two Album details on the stack each get their own (D29).
                rememberViewModelStoreNavEntryDecorator(),
            ),
            // The swipe-back preview is the same fade as every other move; the default shrinks the screen (D29).
            predictivePopTransitionSpec = { _ -> defaultPopTransitionSpec<StackEntry>()(this) },
            entryProvider = { entry -> NavEntry(entry, contentKey = entry.id) { Screen(entry.route, graph, navigator, activityStore) } },
        )
    }
}

@Composable
private fun Screen(route: Route, graph: AppGraph, navigator: Navigator, activityStore: ViewModelStoreOwner) {
    val openAlbum = { album: AlbumEntity -> navigator.open(Route.AlbumDetail(album.id)) }
    val openNowPlaying = { navigator.open(Route.NowPlaying) }
    when (route) {
        Route.Albums -> AlbumsScreen(
            viewModel = viewModel(factory = AlbumsViewModel.factory(graph.session, graph.library)),
            onOpenAlbum = openAlbum,
        )
        Route.Artists -> ArtistsScreen(
            viewModel = viewModel(factory = ArtistsViewModel.factory(graph.library)),
            onOpen = { navigator.open(Route.ArtistDetail(it.id, it.name)) },
        )
        Route.Genres -> GenresScreen(
            viewModel = viewModel(factory = GenresViewModel.factory(graph.library)),
            onOpen = { navigator.open(Route.GenreDetail(it.id, it.name)) },
        )
        Route.Playlists -> PlaylistsScreen(
            viewModel = viewModel(factory = PlaylistsViewModel.factory(graph.library)),
            onOpen = { navigator.open(Route.PlaylistDetail(it.id, it.name)) },
        )
        Route.Search -> SearchScreen(
            viewModel = viewModel(
                viewModelStoreOwner = activityStore,
                factory = SearchViewModel.factory(graph.session, graph.library, graph.playerConnection),
            ),
            onOpenAlbum = openAlbum,
            onPlayed = openNowPlaying,
        )
        is Route.AlbumDetail -> AlbumDetailScreen(
            viewModel = viewModel(
                factory = AlbumDetailViewModel.factory(route.albumId, graph.session, graph.library, graph.playerConnection),
            ),
            onPlayed = openNowPlaying,
            onOpenArtist = { artistId, name -> navigator.open(Route.ArtistDetail(artistId, name)) },
        )
        is Route.ArtistDetail -> ArtistDetailScreen(
            viewModel = viewModel(factory = ArtistDetailViewModel.factory(route.artistId, graph.session, graph.library)),
            name = route.name,
            onOpenAlbum = openAlbum,
        )
        is Route.GenreDetail -> GenreDetailScreen(
            viewModel = viewModel(factory = GenreDetailViewModel.factory(route.genreId, graph.session, graph.library)),
            name = route.name,
            onOpenAlbum = openAlbum,
        )
        is Route.PlaylistDetail -> PlaylistDetailScreen(
            viewModel = viewModel(
                factory = PlaylistDetailViewModel.factory(route.playlistId, graph.session, graph.library, graph.playerConnection),
            ),
            name = route.name,
            onPlayed = openNowPlaying,
        )
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
