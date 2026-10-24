package net.erezschatz.resonancedrift.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel

/**
 * Every destination. Details carry the name they were opened with, so the
 * header shows at once while the rest loads. Playlist detail joins in step 09.
 */
sealed interface Route {
    data object Albums : Route
    data object Artists : Route
    data object Genres : Route
    data object Playlists : Route
    data object Search : Route

    data class AlbumDetail(val albumId: Long) : Route
    data class ArtistDetail(val artistId: Long, val name: String) : Route
    data class GenreDetail(val genreId: Long, val name: String) : Route

    /** Full screen: no mini player, no bottom bar. */
    data object NowPlaying : Route
}

/**
 * One screen on the back stack. [id] is unique per push, so the same route
 * can be on the stack twice (an album, its artist, the album again) and each
 * keeps its own state and ViewModel — NavDisplay keys entries by it.
 */
data class StackEntry(val id: Long, val route: Route)

/** The five bottom-bar tabs, in bar order, each with the route at the bottom of its stack. */
enum class Tab(val label: String, val icon: ImageVector, val root: Route) {
    Albums("Albums", Icons.Filled.Album, Route.Albums),
    Artists("Artists", Icons.Filled.Person, Route.Artists),
    Genres("Genres", Icons.Filled.Category, Route.Genres),
    Playlists("Playlists", Icons.AutoMirrored.Filled.QueueMusic, Route.Playlists),
    Search("Search", Icons.Filled.Search, Route.Search),
}

/**
 * The app's back stack, owned here as plain lists (D15): one stack per tab,
 * so switching tabs keeps each tab where it was. A ViewModel only so it
 * survives rotation.
 *
 * Back follows Android's rule for bottom navigation: pop within the tab; at a
 * tab's root, go to the start tab (Albums); at Albums' root, leave the app.
 * [backStack] is exactly what back will reveal, so predictive back previews
 * the right screen.
 */
class Navigator : ViewModel() {

    var currentTab by mutableStateOf(Tab.Albums)
        private set

    /** Roots take ids 0–4 (one per tab); pushed screens count up from there. */
    private var nextId = Tab.entries.size.toLong()

    private var stacks by mutableStateOf(Tab.entries.associateWith { listOf(StackEntry(it.ordinal.toLong(), it.root)) })

    /** What NavDisplay shows: Albums' stack, with the current tab's stacked on top of it. */
    val backStack: List<StackEntry>
        get() = if (currentTab == Tab.Albums) {
            stackOf(Tab.Albums)
        } else {
            stackOf(Tab.Albums) + stackOf(currentTab)
        }

    val current: Route
        get() = stackOf(currentTab).last().route

    /** A tap on a tab: switch to it, or, if it is already current, back to its root. */
    fun selectTab(tab: Tab) {
        if (tab == currentTab) {
            setStack(tab, stackOf(tab).take(1))
        } else {
            currentTab = tab
        }
    }

    fun open(route: Route) {
        if (current == route) return
        setStack(currentTab, stackOf(currentTab) + StackEntry(nextId++, route))
    }

    /** NavDisplay's onBack; it is only called when [backStack] has more than one entry. */
    fun back() {
        val stack = stackOf(currentTab)
        if (stack.size > 1) {
            setStack(currentTab, stack.dropLast(1))
        } else {
            currentTab = Tab.Albums
        }
    }

    private fun stackOf(tab: Tab): List<StackEntry> = stacks.getValue(tab)

    private fun setStack(tab: Tab, stack: List<StackEntry>) {
        stacks = stacks + (tab to stack)
    }
}
