package net.erezschatz.resonancedrift.ampache

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * The client's handling of shapes the fixtures may not happen to contain:
 * the three single-object forms, string vs number ids, odd values, paging.
 * Hand-written JSON; the real server's JSON is in FixtureParsingTest.
 */
class AmpacheClientTest {

    private lateinit var server: MockWebServer
    private lateinit var serverUrl: String
    private val client = AmpacheClient(OkHttpClient(), AmpacheClient.json)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        serverUrl = server.url("/").toString().trimEnd('/')
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun serve(body: String) {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test
    fun `single object wrapped in an array is unwrapped`() = runTest {
        serve("""{ "album": [ { "id": "12", "name": "Blue" } ] }""")
        assertEquals(12L, client.album(serverUrl, "t", 12).id)
    }

    @Test
    fun `single object under its key is read`() = runTest {
        serve("""{ "album": { "id": 12, "name": "Blue" } }""")
        assertEquals("Blue", client.album(serverUrl, "t", 12).name)
    }

    @Test
    fun `single object at the top level is read`() = runTest {
        serve("""{ "id": "12", "name": "Blue", "artist": { "id": "45", "name": "Joni Mitchell" } }""")
        val album = client.album(serverUrl, "t", 12)
        assertEquals(12L, album.id)
        assertEquals(45L, album.artist?.id)
    }

    @Test
    fun `empty array for a single object is a shape error`() = runTest {
        serve("""{ "album": [] }""")
        try {
            client.album(serverUrl, "t", 12)
            fail("expected NotJson")
        } catch (e: AmpacheException.NotJson) {
            // expected
        }
    }

    @Test
    fun `missing list key is an empty list`() = runTest {
        serve("""{ "total_count": 0 }""")
        assertTrue(client.genreAlbums(serverUrl, "t", 7).isEmpty())
    }

    @Test
    fun `numbers arrive as strings, numbers, empty strings or null`() = runTest {
        serve(
            """{ "song": [
                { "id": "1", "title": "a", "track": "3", "time": 240, "year": "" },
                { "id": 2, "title": "b", "track": null, "time": "181.0", "year": 1971, "genre": [] }
            ] }""",
        )
        val songs = client.albumSongs(serverUrl, "t", 1)
        assertEquals(listOf(1L, 2L), songs.map { it.id })
        assertEquals(listOf(3, 0), songs.map { it.track })
        assertEquals(listOf(240, 181), songs.map { it.time })
        assertEquals(listOf(0, 1971), songs.map { it.year })
    }

    @Test
    fun `non-numeric id is a shape error, not a crash`() = runTest {
        serve("""{ "album": [ { "id": "abc", "name": "x" } ] }""")
        try {
            client.albums(serverUrl, "t", 0, 1)
            fail("expected NotJson")
        } catch (e: AmpacheException.NotJson) {
            // expected
        }
    }

    @Test
    fun `smart playlist ids are kept as strings and passed back as filter`() = runTest {
        serve("""{ "playlist": [ { "id": "smart_3", "name": "Recent", "items": "40" }, { "id": 7, "name": "Driving" } ] }""")
        val playlists = client.playlists(serverUrl, "t", 0, 10)
        assertEquals(listOf("smart_3", "7"), playlists.map { it.id })
        assertEquals(40, playlists[0].items)
        server.takeRequest()

        serve("""{ "song": [] }""")
        client.playlistSongs(serverUrl, "t", "smart_3")
        assertEquals("smart_3", server.takeRequest().requestUrl?.queryParameter("filter"))
    }

    @Test
    fun `write calls succeed on a success envelope and send ReDrift's parameters`() = runTest {
        serve("""{ "success": "song 9 removed from playlist 3" }""")
        client.playlistRemoveSong(serverUrl, "t", "3", 9)
        assertEquals("action=playlist_remove_song&auth=t&filter=3&song=9", server.takeRequest().requestUrl?.encodedQuery)

        serve("""{ "success": "successfully recorded play: 9" }""")
        client.recordPlay(serverUrl, "t", 9)
        assertEquals("action=record_play&auth=t&id=9&client=ResonanceDrift", server.takeRequest().requestUrl?.encodedQuery)
    }

    @Test
    fun `an error envelope on a list action is a ServerError with its code`() = runTest {
        serve("""{ "error": { "errorCode": "4704", "errorAction": "album_songs", "errorType": "filter", "errorMessage": "Not Found: 9" } }""")
        try {
            client.albumSongs(serverUrl, "t", 9)
            fail("expected ServerError")
        } catch (e: AmpacheException.ServerError) {
            assertEquals("4704", e.code)
        }
    }

    // --- paging

    @Test
    fun `paging stops at the first short page`() = runTest {
        val offsets = mutableListOf<Int>()
        val all = AmpacheClient.fetchAllPages(pageSize = 3) { offset, limit ->
            offsets.add(offset)
            assertEquals(3, limit)
            (offset until minOf(offset + limit, 7)).toList()
        }
        assertEquals((0 until 7).toList(), all)
        assertEquals(listOf(0, 3, 6), offsets)
    }

    @Test
    fun `an exact multiple of the page size costs one empty page`() = runTest {
        val offsets = mutableListOf<Int>()
        val all = AmpacheClient.fetchAllPages(pageSize = 3) { offset, limit ->
            offsets.add(offset)
            (offset until minOf(offset + limit, 6)).toList()
        }
        assertEquals(6, all.size)
        assertEquals(listOf(0, 3, 6), offsets)
    }

    @Test
    fun `a server that ignores offset cannot loop forever`() = runTest {
        var calls = 0
        AmpacheClient.fetchAllPages(pageSize = 2) { _, _ ->
            calls++
            listOf(1, 2)
        }
        assertEquals(100, calls)
    }

    @Test
    fun `paging with the real default asks for 5000 at a time`() = runTest {
        val limits = mutableListOf<Int>()
        AmpacheClient.fetchAllPages<Int> { _, limit ->
            limits.add(limit)
            emptyList()
        }
        assertEquals(listOf(5000), limits)
    }
}
