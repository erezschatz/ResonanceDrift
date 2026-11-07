package net.erezschatz.resonancedrift.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayingSongTest {

    private val songs = listOf(11368L, 11370L, 11364L, 11360L, 11370L)

    @Test
    fun `nothing playing, or a song from elsewhere, marks no row`() {
        assertEquals(-1, playingRowIndex(songs, null))
        assertEquals(-1, playingRowIndex(songs, 59490L))
        assertEquals(-1, playingRowIndex(emptyList(), 11360L))
    }

    @Test
    fun `the playing song's row is found, the first one when it is listed twice`() {
        assertEquals(3, playingRowIndex(songs, 11360L))
        assertEquals(1, playingRowIndex(songs, 11370L))
    }

    @Test
    fun `the offset that centres a row is negative, half the spare height`() {
        assertEquals(-450, centeredScrollOffset(viewportHeightPx = 1000, rowHeightPx = 100))
        assertEquals(-1152, centeredScrollOffset(viewportHeightPx = 2460, rowHeightPx = 156))
    }

    @Test
    fun `a row as tall as the viewport, or taller, is not pushed down`() {
        assertEquals(0, centeredScrollOffset(viewportHeightPx = 100, rowHeightPx = 100))
        assertEquals(0, centeredScrollOffset(viewportHeightPx = 100, rowHeightPx = 300))
    }
}
