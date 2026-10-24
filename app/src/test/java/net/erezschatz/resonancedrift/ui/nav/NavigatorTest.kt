package net.erezschatz.resonancedrift.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

/** Back behaves like Android's bottom navigation, and backStack is what back will reveal. */
class NavigatorTest {

    private val navigator = Navigator()

    @Test
    fun `starts on Albums with one screen`() {
        assertEquals(Tab.Albums, navigator.currentTab)
        assertEquals(listOf(Route.Albums), navigator.backStack)
    }

    @Test
    fun `another tab sits on top of Albums, so back from its root goes to Albums`() {
        navigator.selectTab(Tab.Genres)
        assertEquals(listOf(Route.Albums, Route.Genres), navigator.backStack)

        navigator.back()

        assertEquals(Tab.Albums, navigator.currentTab)
        assertEquals(listOf(Route.Albums), navigator.backStack)
    }

    @Test
    fun `back pops within the tab first`() {
        navigator.selectTab(Tab.Search)
        navigator.open(Route.NowPlaying)
        assertEquals(listOf(Route.Albums, Route.Search, Route.NowPlaying), navigator.backStack)

        navigator.back()

        assertEquals(Tab.Search, navigator.currentTab)
        assertEquals(Route.Search, navigator.current)
    }

    @Test
    fun `each tab keeps its own stack across switches`() {
        navigator.open(Route.NowPlaying)
        navigator.selectTab(Tab.Artists)
        assertEquals(listOf(Route.Albums, Route.NowPlaying, Route.Artists), navigator.backStack)

        navigator.selectTab(Tab.Albums)

        assertEquals(listOf(Route.Albums, Route.NowPlaying), navigator.backStack)
    }

    @Test
    fun `tapping the current tab returns it to its root`() {
        navigator.selectTab(Tab.Playlists)
        navigator.open(Route.NowPlaying)

        navigator.selectTab(Tab.Playlists)

        assertEquals(listOf(Route.Albums, Route.Playlists), navigator.backStack)
    }

    @Test
    fun `opening the screen already showing does not stack it twice`() {
        navigator.open(Route.NowPlaying)
        navigator.open(Route.NowPlaying)

        assertEquals(listOf(Route.Albums, Route.NowPlaying), navigator.backStack)
    }
}
