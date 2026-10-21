package net.erezschatz.resonancedrift.ampache

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Every fixture in app/src/test/fixtures/ — verbatim answers from the real
 * server, captured by capture.sh — goes through the real client: HTTP, the
 * envelope check, the [x]-vs-x unwrap and the DTOs. Each test also pins the
 * request the client sent for that action.
 *
 * The assertions are about shape, not content, so a re-capture after the
 * library changes still passes; a re-capture after the *server* changes is
 * exactly when one of these should fail.
 */
class FixtureParsingTest {

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

    @Test
    fun handshake() = runTest {
        serve("handshake")
        val handshake = client.handshake(serverUrl, "user", "hash")
        assertTrue(handshake.auth.isNotEmpty())
        assertTrue(Session.parseExpiry(handshake.sessionExpire) > 0)
        assertEquals("handshake", taken().action)
    }

    @Test
    fun albums() = runTest {
        serve("albums")
        val albums = client.albums(serverUrl, TOKEN, offset = 0, limit = 5)
        assertTrue(albums.isNotEmpty())
        albums.forEach { assertAlbum(it.id, it.name) }
        assertTrue("albums carry their artist", albums.all { it.artist != null })
        assertEquals("action=albums&auth=$TOKEN&offset=0&limit=5", taken().query)
    }

    @Test
    fun album() = runTest {
        serve("album")
        val album = client.album(serverUrl, TOKEN, 123)
        assertAlbum(album.id, album.name)
        assertTrue(album.songcount > 0)
        assertEquals("action=album&auth=$TOKEN&filter=123", taken().query)
    }

    @Test
    fun albumSongs() = runTest {
        serve("album_songs")
        val songs = client.albumSongs(serverUrl, TOKEN, 123)
        assertTrue(songs.isNotEmpty())
        songs.forEach { assertSong(it.id, it.title, it.time) }
        assertEquals("one album", 1, songs.mapNotNull { it.album?.id }.distinct().size)
        assertEquals("action=album_songs&auth=$TOKEN&filter=123", taken().query)
    }

    @Test
    fun artists() = runTest {
        serve("artists")
        val artists = client.artists(serverUrl, TOKEN, offset = 0, limit = 5)
        assertTrue(artists.isNotEmpty())
        artists.forEach { assertNamed(it.id, it.name) }
        assertEquals("action=artists&auth=$TOKEN&offset=0&limit=5", taken().query)
    }

    @Test
    fun artist() = runTest {
        serve("artist")
        val artist = client.artist(serverUrl, TOKEN, 45)
        assertNamed(artist.id, artist.name)
        assertTrue(artist.albumcount > 0)
        assertEquals("action=artist&auth=$TOKEN&filter=45", taken().query)
    }

    @Test
    fun artistAlbums() = runTest {
        serve("artist_albums")
        val albums = client.artistAlbums(serverUrl, TOKEN, 45)
        assertTrue(albums.isNotEmpty())
        albums.forEach { assertAlbum(it.id, it.name) }
        assertEquals("action=artist_albums&auth=$TOKEN&filter=45", taken().query)
    }

    @Test
    fun genres() = runTest {
        serve("genres")
        val genres = client.genres(serverUrl, TOKEN, offset = 0, limit = 5)
        assertTrue(genres.isNotEmpty())
        genres.forEach { assertNamed(it.id, it.name) }
        assertEquals("action=genres&auth=$TOKEN&offset=0&limit=5", taken().query)
    }

    @Test
    fun genre() = runTest {
        serve("genre")
        val genre = client.genre(serverUrl, TOKEN, 7)
        assertNamed(genre.id, genre.name)
        assertTrue(genre.albums > 0)
        assertEquals("action=genre&auth=$TOKEN&filter=7", taken().query)
    }

    @Test
    fun genreAlbums() = runTest {
        serve("genre_albums")
        val albums = client.genreAlbums(serverUrl, TOKEN, 7)
        assertTrue(albums.isNotEmpty())
        albums.forEach { assertAlbum(it.id, it.name) }
        assertEquals("action=genre_albums&auth=$TOKEN&filter=7", taken().query)
    }

    @Test
    fun genreSongs() = runTest {
        serve("genre_songs")
        val songs = client.genreSongs(serverUrl, TOKEN, 7)
        assertTrue(songs.isNotEmpty())
        songs.forEach { assertSong(it.id, it.title, it.time) }
        assertEquals("action=genre_songs&auth=$TOKEN&filter=7", taken().query)
    }

    @Test
    fun playlists() = runTest {
        serve("playlists")
        val playlists = client.playlists(serverUrl, TOKEN, offset = 0, limit = 5)
        assertTrue(playlists.isNotEmpty())
        playlists.forEach {
            assertTrue("playlist id", it.id.isNotEmpty())
            assertTrue("playlist name", it.name.isNotBlank())
        }
        assertEquals("action=playlists&auth=$TOKEN&offset=0&limit=5", taken().query)
    }

    @Test
    fun playlist() = runTest {
        serve("playlist")
        val playlist = client.playlist(serverUrl, TOKEN, "3")
        assertTrue(playlist.id.isNotEmpty())
        assertTrue(playlist.items > 0)
        assertEquals("action=playlist&auth=$TOKEN&filter=3", taken().query)
    }

    @Test
    fun playlistSongs() = runTest {
        serve("playlist_songs")
        val songs = client.playlistSongs(serverUrl, TOKEN, "3")
        assertTrue(songs.isNotEmpty())
        songs.forEach { assertSong(it.id, it.title, it.time) }
        assertEquals("action=playlist_songs&auth=$TOKEN&filter=3", taken().query)
    }

    @Test
    fun song() = runTest {
        serve("song")
        val song = client.song(serverUrl, TOKEN, 9876)
        assertSong(song.id, song.title, song.time)
        assertTrue("song knows its album", song.album != null)
        assertTrue("song knows its artist", song.artist != null)
        assertEquals("action=song&auth=$TOKEN&filter=9876", taken().query)
    }

    @Test
    fun advancedSearchAlbumByTitle() = runTest {
        serve("advanced_search_album_title")
        val albums = client.searchAlbums(serverUrl, TOKEN, rule = "title", query = "Blue")
        assertTrue(albums.isNotEmpty())
        albums.forEach { assertAlbum(it.id, it.name) }
        assertEquals(
            "action=advanced_search&auth=$TOKEN&type=album&operator=and&rule_1=title" +
                "&rule_1_operator=0&rule_1_input=Blue&offset=0&limit=50",
            taken().query,
        )
    }

    @Test
    fun advancedSearchSongByTitle() = runTest {
        serve("advanced_search_song_title")
        val songs = client.searchSongs(serverUrl, TOKEN, query = "River")
        assertTrue(songs.isNotEmpty())
        songs.forEach { assertSong(it.id, it.title, it.time) }
        assertTrue("search songs carry their album, for the albums-by-song merge", songs.all { it.album != null })
        assertEquals(
            "action=advanced_search&auth=$TOKEN&type=song&operator=and&rule_1=title" +
                "&rule_1_operator=0&rule_1_input=River&offset=0&limit=50",
            taken().query,
        )
    }

    @Test
    fun errorSessionExpired() = runTest {
        serve("error_session_expired")
        try {
            client.albums(serverUrl, TOKEN, offset = 0, limit = 1)
            fail("expected SessionExpired")
        } catch (e: AmpacheException.SessionExpired) {
            // the envelope, recognised
        }
    }

    // --- helpers

    private fun serve(name: String) {
        val file = File(FIXTURES, "$name.json")
        if (!file.exists()) {
            fail("Missing fixture ${file.path} — run app/src/test/fixtures/capture.sh against the real server")
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody(file.readText()),
        )
    }

    private fun taken(): RecordedRequest = server.takeRequest()

    private val RecordedRequest.action: String?
        get() = requestUrl?.queryParameter("action")

    private val RecordedRequest.query: String?
        get() = requestUrl?.encodedQuery

    private fun assertNamed(id: Long, name: String) {
        assertTrue("id $id", id > 0)
        assertTrue("name of $id", name.isNotBlank())
    }

    private fun assertAlbum(id: Long, name: String) = assertNamed(id, name)

    private fun assertSong(id: Long, title: String, seconds: Int) {
        assertNamed(id, title)
        assertTrue("duration of $id", seconds > 0)
    }

    companion object {
        /** Gradle runs unit tests with the module (app/) as working directory. */
        private val FIXTURES = File("src/test/fixtures")
        private const val TOKEN = "tok"
    }
}
