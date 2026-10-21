package net.erezschatz.resonancedrift.ampache

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SessionTest {

    private lateinit var server: MockWebServer
    private lateinit var serverUrl: String
    private val store = FakeCredentialStore()
    private val client = AmpacheClient(OkHttpClient(), Json { ignoreUnknownKeys = true })

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

    // --- hashing

    @Test
    fun `sha256 matches the standard vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AmpacheClient.sha256Hex("abc"),
        )
    }

    @Test
    fun `handshake auth is sha256 of username plus sha256 of password`() {
        // demo / demodemo, computed independently with Python's hashlib.
        val passwordHash = AmpacheClient.sha256Hex("demodemo")
        assertEquals("4011ba02e51104c678c31a76a444485b38f3865e504d89c3a0a80b439bb1a237", passwordHash)
        assertEquals(
            "e131647f22af624963e273c63659da1451d4e7f52442ec5362e660f9628bd2da",
            AmpacheClient.sha256Hex("demo" + passwordHash),
        )
    }

    // --- expiry maths

    @Test
    fun `session_expire with offset parses to epoch millis`() {
        assertEquals(1_791_817_445_000L, Session.parseExpiry("2026-10-12T15:04:05+00:00"))
        assertEquals(1_791_810_245_000L, Session.parseExpiry("2026-10-12T15:04:05+02:00"))
    }

    @Test
    fun `ping only when under 20 minutes remain and not within a minute of the last check`() {
        val minute = 60_000L
        val now = 100 * minute
        assertTrue(Session.shouldPing(now, now + 19 * minute, lastCheckMillis = 0))
        assertFalse(Session.shouldPing(now, now + 21 * minute, lastCheckMillis = 0))
        assertTrue("already expired still pings", Session.shouldPing(now, now - minute, lastCheckMillis = 0))
        assertFalse("throttled", Session.shouldPing(now, now + minute, lastCheckMillis = now - 30_000))
        assertTrue(Session.shouldPing(now, now + minute, lastCheckMillis = now - minute))
    }

    // --- login and restore

    @Test
    fun `login stores the password hash, never the password`() = runTest {
        server.enqueue(json(handshakeBody("tok1")))
        val session = Session(client, store)
        session.restore()

        session.login(serverUrl, "demo", "demodemo")

        val saved = savedLogin()
        assertEquals(AmpacheClient.sha256Hex("demodemo"), saved.passwordHash)
        assertEquals("tok1", saved.token)
        assertEquals(SessionState.LoggedIn(serverUrl, "demo"), session.state.value)
        val request = takeUrl()
        assertEquals("handshake", request.queryParameter("action"))
        assertEquals(
            AmpacheClient.sha256Hex("demo" + AmpacheClient.sha256Hex("demodemo")),
            request.queryParameter("auth"),
        )
        assertEquals("6.0.0", request.queryParameter("version"))
    }

    @Test
    fun `restore with stored credentials is logged in without any request`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "tok", 0)
        val session = Session(client, store)
        session.restore()
        assertEquals(SessionState.LoggedIn(serverUrl, "demo"), session.state.value)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `wrong credentials read plainly, server detail kept for logs`() = runTest {
        server.enqueue(json("""{"error":{"errorCode":"4701","errorMessage":"Received Invalid Handshake - Incorrect username or password"}}"""))
        val session = Session(client, store)
        session.restore()
        try {
            session.login(serverUrl, "demo", "nope")
            fail("expected ServerError")
        } catch (e: AmpacheException.ServerError) {
            assertEquals("Wrong username or password.", e.userMessage)
            assertTrue(e.message.orEmpty().contains("Incorrect username or password"))
        }
        assertNull(store.saved)
    }

    @Test
    fun `an html page instead of json is reported as not json`() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<html>login</html>"))
        val session = Session(client, store)
        session.restore()
        try {
            session.login(serverUrl, "demo", "demodemo")
            fail("expected NotJson")
        } catch (e: AmpacheException.NotJson) {
            assertTrue(e.message.orEmpty().contains("text/html"))
        }
    }

    @Test
    fun `logout forgets the secrets but keeps server and username`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "tok", 0)
        val session = Session(client, store)
        session.restore()
        session.logout()
        assertNull(store.saved)
        assertEquals(LastAccount(serverUrl, "demo"), store.lastAccount())
        assertEquals(SessionState.LoggedOut(serverUrl, "demo"), session.state.value)
    }

    @Test
    fun `restart after logout shows the auth screen prefilled`() = runTest {
        store.last = LastAccount(serverUrl, "demo")
        val session = Session(client, store)
        session.restore()
        assertEquals(SessionState.LoggedOut(serverUrl, "demo"), session.state.value)
    }

    @Test
    fun `fresh install shows an empty auth screen`() = runTest {
        val session = Session(client, store)
        session.restore()
        assertEquals(SessionState.LoggedOut("", ""), session.state.value)
    }

    // --- re-auth then retry

    @Test
    fun `expired session re-handshakes and retries the call once`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "old", 0)
        server.enqueue(json("""{"error":{"errorCode":"4701","errorMessage":"Session Expired"}}"""))
        server.enqueue(json(handshakeBody("new")))
        server.enqueue(json(pingBody()))
        val session = Session(client, store)
        session.restore()

        session.call { s, token -> client.ping(s, token) }

        assertEquals("old", takeUrl().queryParameter("auth"))
        val handshake = takeUrl()
        assertEquals("handshake", handshake.queryParameter("action"))
        assertEquals(AmpacheClient.sha256Hex("demo" + "hash"), handshake.queryParameter("auth"))
        assertEquals("new", takeUrl().queryParameter("auth"))
        assertEquals("new", savedLogin().token)
    }

    @Test
    fun `renewToken re-handshakes without a failing call and the resolver sees the new token`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "old", 0)
        server.enqueue(json(handshakeBody("new")))
        val session = Session(client, store)
        session.restore()

        session.renewToken()

        assertEquals("handshake", takeUrl().queryParameter("action"))
        assertEquals("new", session.currentLogin()?.token)
        assertEquals("new", savedLogin().token)
    }

    @Test
    fun `http 401 counts as expiry too`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "old", 0)
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(json(handshakeBody("new")))
        server.enqueue(json(pingBody()))
        val session = Session(client, store)
        session.restore()

        session.call { s, token -> client.ping(s, token) }

        assertEquals(3, server.requestCount)
        assertEquals("new", savedLogin().token)
    }

    @Test
    fun `a second expiry after re-auth is thrown, not retried forever`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "old", 0)
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(json(handshakeBody("new")))
        server.enqueue(MockResponse().setResponseCode(403))
        val session = Session(client, store)
        session.restore()
        try {
            session.call { s, token -> client.ping(s, token) }
            fail("expected SessionExpired")
        } catch (e: AmpacheException.SessionExpired) {
            // expected
        }
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `rejected re-handshake logs out but keeps server and username`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "old", 0)
        server.enqueue(json("""{"error":{"errorCode":"4701","errorMessage":"Session Expired"}}"""))
        server.enqueue(json("""{"error":{"errorCode":"4701","errorMessage":"Received Invalid Handshake - Incorrect username or password"}}"""))
        val session = Session(client, store)
        session.restore()
        try {
            session.call { s, token -> client.ping(s, token) }
            fail("expected ServerError")
        } catch (e: AmpacheException.ServerError) {
            // expected
        }
        assertNull(store.saved)
        assertEquals(SessionState.LoggedOut(serverUrl, "demo"), session.state.value)
    }

    @Test
    fun `ping without session_expire means the token is dead`() = runTest {
        store.saved = StoredLogin(serverUrl, "demo", "hash", "old", 0)
        server.enqueue(json("""{"server":"6.6.0","version":"6.6.0","compatible":"350001"}"""))
        server.enqueue(json(handshakeBody("new")))
        server.enqueue(json(pingBody()))
        val session = Session(client, store)
        session.restore()

        session.pingIfStale()

        assertEquals(3, server.requestCount)
        assertEquals("new", savedLogin().token)
        assertEquals(Session.parseExpiry(EXPIRE), savedLogin().expiresAtMillis)
    }

    // --- helpers

    private fun savedLogin(): StoredLogin = checkNotNull(store.saved) { "nothing saved" }

    private fun takeUrl(): HttpUrl = checkNotNull(server.takeRequest().requestUrl)

    private fun json(body: String) =
        MockResponse().setHeader("Content-Type", "application/json; charset=UTF-8").setBody(body)

    private fun handshakeBody(token: String) =
        """{"auth":"$token","api":"6.6.0","session_expire":"$EXPIRE","songs":10,"albums":2}"""

    private fun pingBody() = """{"session_expire":"$EXPIRE","server":"6.6.0"}"""

    /** Mirrors Settings: the secrets and the last account are stored separately. */
    private class FakeCredentialStore : CredentialStore {
        var saved: StoredLogin? = null
        var last: LastAccount? = null
        override suspend fun load() = saved
        override suspend fun save(login: StoredLogin) {
            saved = login
            last = LastAccount(login.serverUrl, login.username)
        }
        override suspend fun lastAccount() = last ?: saved?.let { LastAccount(it.serverUrl, it.username) }
        override suspend fun clearSecrets() {
            last = lastAccount()
            saved = null
        }
    }

    private companion object {
        const val EXPIRE = "2026-10-12T15:04:05+00:00"
    }
}
