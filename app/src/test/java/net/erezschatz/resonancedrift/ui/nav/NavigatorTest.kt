package net.erezschatz.resonancedrift.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

/** Back behaves like Android's bottom navigation, and backStack is what back will reveal. */
class NavigatorTest {

    private val navigator = Navigator()

    private fun routes() = navigator.backStack.map { it.route }

    @Test
    fun `starts on Albums with one screen`() {
        assertEquals(Tab.Albums, navigator.currentTab)
        assertEquals(listOf(Route.Albums), routes())
    }

    @Test
    fun `another tab sits on top of Albums, so back from its root goes to Albums`() {
        navigator.selectTab(Tab.Genres)
        assertEquals(listOf(Route.Albums, Route.Genres), routes())

        navigator.back()

        assertEquals(Tab.Albums, navigator.currentTab)
        assertEquals(listOf(Route.Albums), routes())
    }

    @Test
    fun `back pops within the tab first`() {
        navigator.selectTab(Tab.Search)
        navigator.open(Route.NowPlaying)
        assertEquals(listOf(Route.Albums, Route.Search, Route.NowPlaying), routes())

        navigator.back()

        assertEquals(Tab.Search, navigator.currentTab)
        assertEquals(Route.Search, navigator.current)
    }

    @Test
    fun `each tab keeps its own stack across switches`() {
        navigator.open(Route.NowPlaying)
        navigator.selectTab(Tab.Artists)
        assertEquals(listOf(Route.Albums, Route.NowPlaying, Route.Artists), routes())

        navigator.selectTab(Tab.Albums)

        assertEquals(listOf(Route.Albums, Route.NowPlaying), routes())
    }

    @Test
    fun `tapping the current tab returns it to its root`() {
        navigator.selectTab(Tab.Playlists)
        navigator.open(Route.NowPlaying)

        navigator.selectTab(Tab.Playlists)

        assertEquals(listOf(Route.Albums, Route.Playlists), routes())
    }

    @Test
    fun `opening the screen already showing does not stack it twice`() {
        navigator.open(Route.NowPlaying)
        navigator.open(Route.NowPlaying)

        assertEquals(listOf(Route.Albums, Route.NowPlaying), routes())
    }

    @Test
    fun `detail screens stack inside a tab and back unwinds them one at a time`() {
        navigator.selectTab(Tab.Artists)
        navigator.open(Route.ArtistDetail(7, "Artist"))
        navigator.open(Route.AlbumDetail(70))
        navigator.open(Route.ArtistDetail(8, "Another"))
        assertEquals(
            listOf(Route.Albums, Route.Artists, Route.ArtistDetail(7, "Artist"), Route.AlbumDetail(70), Route.ArtistDetail(8, "Another")),
            routes(),
        )

        navigator.back()
        navigator.back()

        assertEquals(Route.ArtistDetail(7, "Artist"), navigator.current)
        assertEquals(Tab.Artists, navigator.currentTab)
    }

    @Test
    fun `the same album can be on the stack twice when reached by different paths`() {
        navigator.open(Route.AlbumDetail(70))
        navigator.open(Route.ArtistDetail(7, "Artist"))
        navigator.open(Route.AlbumDetail(70))

        assertEquals(4, routes().size)
        assertEquals("every entry has its own id", 4, navigator.backStack.map { it.id }.distinct().size)
    }
}
