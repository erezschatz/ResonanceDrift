package net.erezschatz.resonancedrift.ampache

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumNamesTest {

    @Test
    fun `plain album name is untouched`() {
        assertEquals("Blue", AlbumNames.display("Blue", "Joni Mitchell"))
    }

    @Test
    fun `artist prefix is stripped when it matches the artist`() {
        assertEquals("Greatest Hits", AlbumNames.display("Queen - Greatest Hits", "Queen"))
    }

    @Test
    fun `artist prefix is kept when it does not match the artist`() {
        assertEquals("Queen - Greatest Hits", AlbumNames.display("Queen - Greatest Hits", "ABBA"))
    }

    @Test
    fun `with no artist, everything after the first separator is the album`() {
        assertEquals("Greatest Hits", AlbumNames.display("Queen - Greatest Hits", null))
        assertEquals("Greatest Hits", AlbumNames.display("Queen - Greatest Hits", ""))
    }

    @Test
    fun `a dash inside the album title survives`() {
        assertEquals("Live - At Wembley", AlbumNames.display("Queen - Live - At Wembley", "Queen"))
        assertEquals("Live - At Wembley", AlbumNames.display("Queen - Live - At Wembley", null))
    }
}
