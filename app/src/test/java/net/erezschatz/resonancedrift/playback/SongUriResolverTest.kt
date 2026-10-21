package net.erezschatz.resonancedrift.playback

import net.erezschatz.resonancedrift.ampache.StoredLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SongUriResolverTest {

    private var login: StoredLogin? = StoredLogin("https://music.example.com", "erez", "hash", "token-1", 0L)
    private val resolver = SongUriResolver { login }

    @Test
    fun `song uri resolves to the stream url with the current token`() {
        assertEquals(
            "https://music.example.com/play/index.php?ssid=token-1&type=song&oid=58397&format=raw",
            resolver.streamUrl("rd://song/58397"),
        )
    }

    @Test
    fun `a token change shows on the next resolve without touching the item`() {
        val uri = SongUriResolver.uriFor(58397)
        resolver.streamUrl(uri)
        login = login?.copy(token = "token-2")
        assertEquals(
            "https://music.example.com/play/index.php?ssid=token-2&type=song&oid=58397&format=raw",
            resolver.streamUrl(uri),
        )
    }

    @Test
    fun `other uris are left alone`() {
        assertNull(resolver.streamUrl("https://example.com/a.mp3"))
        assertNull(resolver.streamUrl("rd://song/not-a-number"))
        assertNull(resolver.streamUrl("rd://art/12"))
    }

    @Test
    fun `logged out is an IO error, so the load fails like any network failure`() {
        login = null
        try {
            resolver.streamUrl("rd://song/1")
            fail("expected IOException")
        } catch (e: IOException) {
            // expected
        }
    }

    @Test
    fun `uri round-trips the song id`() {
        assertEquals("rd://song/42", SongUriResolver.uriFor(42))
        assertEquals(42L, SongUriResolver.songIdFrom("rd://song/42"))
    }
}
