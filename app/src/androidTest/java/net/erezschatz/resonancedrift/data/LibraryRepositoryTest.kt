package net.erezschatz.resonancedrift.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.erezschatz.resonancedrift.ampache.AmpacheClient
import net.erezschatz.resonancedrift.ampache.AmpacheException
import net.erezschatz.resonancedrift.ampache.CredentialStore
import net.erezschatz.resonancedrift.ampache.LastAccount
import net.erezschatz.resonancedrift.ampache.Session
import net.erezschatz.resonancedrift.ampache.StoredLogin
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Refetches against a fake server: the cached lists follow what the server
 * says now. The "server" is an OkHttp interceptor, so no socket is opened.
 */
@RunWith(AndroidJUnit4::class)
class LibraryRepositoryTest {

    @get:Rule val rule = DbTestRule()
    private lateinit var repository: LibraryRepository

    /** What the fake server answers to playlist_songs, by request number (0-based). */
    private var playlistAnswers: (Int) -> List<Long> = { emptyList() }
    private var artistsAnswer: List<String> = emptyList()
    private var playlistSongsRequests = 0

    /** advanced_search answers by "type/rule"; a missing key answers with an error envelope. */
    private var searchAnswers: Map<String, String> = emptyMap()

    /** playlist_remove_song: null answers success, otherwise this error envelope's message. */
    private var removeRefusal: String? = null

    @Before
    fun setUp() = runBlocking {
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> answer(chain) }).build()
        val client = AmpacheClient(http, AmpacheClient.json)
        val login = StoredLogin("https://music.test", "erez", "hash", "token", Long.MAX_VALUE)
        val session = Session(client, FixedLogin(login))
        session.restore()
        repository = LibraryRepository(session, client, rule.db.library(), rule.db.playlistSongs())
    }

    private fun answer(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        val body = when (url.queryParameter("action")) {
            "advanced_search" -> searchAnswers["${url.queryParameter("type")}/${url.queryParameter("rule_1")}"]
                ?: """{"error":{"errorCode":"4710","errorMessage":"search broke"}}"""
            "playlist_songs" -> songsJson(playlistAnswers(playlistSongsRequests++))
            "artists" -> artistsJson(artistsAnswer)
            "playlist_remove_song" -> removeRefusal?.let { """{"error":{"errorCode":"4742","errorMessage":"$it"}}""" }
                ?: """{"success":"song removed from playlist"}"""
            "genre_albums" -> """{"album":[
                {"id":"1","name":"b later","year":"2010"},
                {"id":"2","name":"Zed","year":"1999"},
                {"id":"3","name":"a later","year":"2010"},
                {"id":"4","name":"No year","year":"0"}]}"""
            else -> """{"error":{"errorCode":"4705","errorMessage":"unexpected action"}}"""
        }
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Type", "application/json")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    @Test
    fun artistsRemovedOnTheServerLeaveTheCachedList() = runTest {
        artistsAnswer = listOf("Alpha", "Beta", "Gamma")
        repository.artists().first { it.refresh == Refresh.Done }

        artistsAnswer = listOf("Alpha", "Gamma", "Delta")
        val refetched = repository.artists().first { it.refresh == Refresh.Done }

        assertEquals(listOf("Alpha", "Delta", "Gamma"), refetched.items.map { it.name })
    }

    @Test
    fun playlistSongsFollowTheServerAcrossRefetches() = runTest {
        playlistAnswers = { request -> if (request == 0) listOf(1, 2, 3) else listOf(3, 4) }

        repository.playlistSongs("12").first { it.refresh == Refresh.Done }
        val refetched = repository.playlistSongs("12").first { it.refresh == Refresh.Done }

        assertEquals(listOf(3L, 4L), refetched.items.map { it.id })
    }

    @Test
    fun aRemovalDuringTheFetchIsKeptAndTheSecondFetchBringsTheServersList() = runTest {
        rule.db.playlistSongs().replaceIfUnchanged("12", 0, listOf(1, 2, 3))
        playlistAnswers = { request ->
            if (request == 0) {
                // The user removes song 2 while this first answer is on its way.
                runBlocking { rule.db.playlistSongs().removeSong("12", index = 1, songId = 2) }
                listOf(1, 2, 3)
            } else {
                // Meanwhile the server also gained a song from elsewhere.
                listOf(1, 3, 5)
            }
        }

        val listing = repository.playlistSongs("12").first { it.refresh == Refresh.Done }

        assertEquals(2, playlistSongsRequests)
        assertEquals(listOf(1L, 3L, 5L), listing.items.map { it.id })
    }

    @Test
    fun aRemovalTheServerAcceptsLeavesTheCachedList() = runTest {
        rule.db.playlistSongs().replaceIfUnchanged("12", 0, listOf(1, 2, 3))

        repository.removeFromPlaylist("12", index = 1, songId = 2)

        assertEquals(listOf(1L, 3L), rule.db.playlistSongs().get("12")?.songIds)
    }

    @Test
    fun aRemovalTheServerRefusesLeavesTheCacheAlone() = runTest {
        rule.db.playlistSongs().replaceIfUnchanged("12", 0, listOf(1, 2, 3))
        removeRefusal = "Access denied"

        val thrown = runCatching { repository.removeFromPlaylist("12", index = 1, songId = 2) }.exceptionOrNull()

        assertTrue(thrown is AmpacheException)
        assertEquals(listOf(1L, 2L, 3L), rule.db.playlistSongs().get("12")?.songIds)
        assertEquals("version unchanged", 1, rule.db.playlistSongs().versionOf("12"))
    }

    @Test
    fun searchMergesAlbumsAndKeepsTheSongs() = runTest {
        searchAnswers = mapOf(
            "album/title" to """{"album":[{"id":"1","name":"Blue"},{"id":"2","name":"Blue Train"}]}""",
            "album/artist" to """{"album":[{"id":"2","name":"Blue Train"},{"id":"3","name":"Kind of Blue"}]}""",
            "song/title" to """{"song":[{"id":"30","title":"Blue in Green","album":{"id":"3","name":"Kind of Blue"}},
                {"id":"40","title":"Blues","album":{"id":"4","name":"Elsewhere"}}]}""",
        )

        val result = repository.search("blue")

        assertEquals(listOf(1L, 2L, 3L, 4L), result.albums.map { it.id })
        assertEquals(listOf(30L, 40L), result.songs.map { it.id })
        assertEquals(null, result.albumsError)
        assertEquals(null, result.songsError)
    }

    @Test
    fun aFailedQueryDegradesOnlyItsSection() = runTest {
        searchAnswers = mapOf(
            "album/title" to """{"album":[{"id":"1","name":"Blue"}]}""",
            // album/artist and song/title answer with an error envelope
        )

        val result = repository.search("blue")

        assertEquals(listOf(1L), result.albums.map { it.id })
        assertTrue(result.albumsError is AmpacheException)
        assertTrue(result.songs.isEmpty())
        assertTrue(result.songsError is AmpacheException)
    }

    @Test
    fun genreAlbumsComeSortedByYearThenName() = runTest {
        val albums = repository.genreAlbums(genreId = 5)

        assertEquals(listOf("No year", "Zed", "a later", "b later"), albums.map { it.name })
    }

    private fun songsJson(ids: List<Long>): String {
        val songs = ids.joinToString(",") { """{"id":"$it","title":"Song $it"}""" }
        return """{"song":[$songs]}"""
    }

    private fun artistsJson(names: List<String>): String {
        val artists = names.mapIndexed { i, name -> """{"id":"${i + 1}${name.length}","name":"$name"}""" }
        return """{"artist":[${artists.joinToString(",")}]}"""
    }

    private class FixedLogin(private val login: StoredLogin) : CredentialStore {
        override suspend fun load() = login
        override suspend fun save(login: StoredLogin) {}
        override suspend fun lastAccount() = LastAccount(login.serverUrl, login.username)
        override suspend fun clearSecrets() {}
    }
}
