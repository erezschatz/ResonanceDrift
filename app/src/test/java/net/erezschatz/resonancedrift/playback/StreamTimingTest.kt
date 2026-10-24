package net.erezschatz.resonancedrift.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StreamTimingTest {

    @get:Rule val folder = TemporaryFolder()
    private lateinit var server: MockWebServer
    private lateinit var log: PlaybackLog
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        log = PlaybackLog(folder.newFolder("log"))
        log.startRun("run started")
        client = OkHttpClient.Builder().eventListenerFactory(StreamTiming.factory(log)).build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a stream request logs its timing, the song id and the connection, not the token`() {
        server.enqueue(MockResponse().setBody("audio"))
        server.enqueue(MockResponse().setBody("audio"))

        fetch("/play/index.php?ssid=SECRET&type=song&oid=58040&format=raw")
        fetch("/play/index.php?ssid=SECRET&type=song&oid=58041&format=raw")

        val lines = log.entries().map { it.text }.drop(1)
        assertEquals(2, lines.size)
        assertTrue(lines[0], lines[0].matches(Regex("stream song-58040: answered in \\d+ ms, first bytes at \\d+ ms \\(new connection in \\d+ ms\\)")))
        assertTrue("the second request reuses the connection: ${lines[1]}", lines[1].endsWith("(reused connection)"))
        assertFalse(lines.any { it.contains("SECRET") })
    }

    @Test
    fun `api and art requests are not timed`() {
        server.enqueue(MockResponse().setBody("{}"))

        fetch("/server/json.server.php?action=ping&auth=SECRET")

        assertEquals(listOf("run started"), log.entries().map { it.text })
    }

    private fun fetch(path: String) {
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().use { it.body.string() }
    }
}
