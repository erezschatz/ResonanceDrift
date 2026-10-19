package net.erezschatz.resonancedrift.ampache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmpacheUrlsTest {

    @Test
    fun `server input is trimmed, slash-stripped and given https when bare`() {
        assertEquals("https://music.example.com", AmpacheUrls.normalizeServer("  https://music.example.com/ "))
        assertEquals("https://music.example.com", AmpacheUrls.normalizeServer("music.example.com"))
        assertEquals("http://10.0.0.5:8080/ampache", AmpacheUrls.normalizeServer("http://10.0.0.5:8080/ampache//"))
    }

    @Test
    fun `garbage server input is rejected`() {
        assertNull(AmpacheUrls.normalizeServer(""))
        assertNull(AmpacheUrls.normalizeServer("   "))
        assertNull(AmpacheUrls.normalizeServer("not a url"))
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
}
