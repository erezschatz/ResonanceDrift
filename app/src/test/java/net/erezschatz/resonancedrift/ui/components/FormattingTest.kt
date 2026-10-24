package net.erezschatz.resonancedrift.ui.components

import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.GenreTag
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `durations read like a player shows them`() {
        assertEquals("0:38", formatDuration(38))
        assertEquals("5:28", formatDuration(328))
        assertEquals("1:02:05", formatDuration(3725))
        assertEquals("", formatDuration(0))
    }

    @Test
    fun `counts leave out what the server did not send`() {
        assertEquals("12 albums · 140 songs", countsLine(12, 140))
        assertEquals("1 album · 1 song", countsLine(1, 1))
        assertEquals("140 songs", countsLine(0, 140))
        assertEquals("", countsLine(0, 0))
    }

    @Test
    fun `album subtitle is artist and year, or whichever is known`() {
        assertEquals("Artist · 2003", artistAndYear(album("Artist", 2003)))
        assertEquals("Artist", artistAndYear(album("Artist", 0)))
        assertEquals("2003", artistAndYear(album("", 2003)))
    }

    @Test
    fun `genre line shows none, one, or the first two of three`() {
        assertEquals("", genreLine(emptyList()))
        assertEquals("Rock", genreLine(listOf(GenreTag(1, "Rock"))))
        assertEquals("Rock · Folk", genreLine(listOf(GenreTag(1, "Rock"), GenreTag(2, "Folk"))))
        assertEquals("Rock · Folk", genreLine(listOf(GenreTag(1, "Rock"), GenreTag(2, "Folk"), GenreTag(3, "Jazz"))))
    }

    private fun album(artist: String, year: Int) = AlbumEntity(1, "Name", 2, artist, year, 10, 2400)
}
