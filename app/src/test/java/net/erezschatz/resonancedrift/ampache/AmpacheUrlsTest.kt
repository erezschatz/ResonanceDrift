package net.erezschatz.resonancedrift.ampache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmpacheUrlsTest {

    @Test
    fun `server input loses whitespace and trailing slashes, and given https when bare`() {
        assertEquals("https://music.example.com", AmpacheUrls.normalizeServer("  https://music.example.com/ "))
        assertEquals("https://music.example.com", AmpacheUrls.normalizeServer("music.example.com"))
        assertEquals("http://10.0.0.5:8080/ampache", AmpacheUrls.normalizeServer("http://10.0.0.5:8080/ampache//"))
        assertEquals("https://music.example.com", AmpacheUrls.normalizeServer("music. example. com"))
    }

    @Test
    fun `garbage server input is rejected`() {
        assertNull(AmpacheUrls.normalizeServer(""))
        assertNull(AmpacheUrls.normalizeServer("   "))
        assertNull(AmpacheUrls.normalizeServer("music.example.com:99999"))
    }

    @Test
    fun `handshake url matches ReDrift byte for byte`() {
        val url = AmpacheUrls.api("https://music.example.com", "handshake", "abc123", listOf("version" to "6.0.0"))
        assertEquals(
            "https://music.example.com/server/json.server.php?action=handshake&auth=abc123&version=6.0.0",
            url.toString(),
        )
    }

    @Test
    fun `api url keeps a server installed under a path`() {
        val url = AmpacheUrls.api("https://example.com/ampache", "ping", "tok")
        assertEquals("https://example.com/ampache/server/json.server.php?action=ping&auth=tok", url.toString())
    }

    @Test
    fun `parameters are encoded`() {
        val url = AmpacheUrls.api("https://h", "advanced_search", "t", listOf("rule_1_input" to "AC/DC & co"))
        assertEquals(
            "https://h/server/json.server.php?action=advanced_search&auth=t&rule_1_input=AC%2FDC%20%26%20co",
            url.toString(),
        )
    }

    // Expected strings are what ReDrift's getStreamUrl() / getArtUrl() return
    // for the same inputs (string concatenation and URLSearchParams, respectively).

    @Test
    fun `stream url matches ReDrift byte for byte`() {
        assertEquals(
            "https://music.example.com/play/index.php?ssid=abc123&type=song&oid=9876&format=raw",
            AmpacheUrls.stream("https://music.example.com", "abc123", 9876),
        )
    }

    @Test
    fun `stream url keeps a server installed under a path`() {
        assertEquals(
            "https://example.com/ampache/play/index.php?ssid=t&type=song&oid=1&format=raw",
            AmpacheUrls.stream("https://example.com/ampache", "t", 1),
        )
    }

    @Test
    fun `art url matches ReDrift byte for byte, default size 300`() {
        assertEquals(
            "https://music.example.com/server/json.server.php?action=get_art&auth=abc123&id=123&type=album&size=300",
            AmpacheUrls.art("https://music.example.com", "abc123", 123),
        )
    }

    @Test
    fun `art url takes a size`() {
        assertEquals(
            "https://h/server/json.server.php?action=get_art&auth=t&id=5&type=album&size=512",
            AmpacheUrls.art("https://h", "t", 5, sizePx = 512),
        )
    }
}
