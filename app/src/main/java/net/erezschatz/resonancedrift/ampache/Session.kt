package net.erezschatz.resonancedrift.ampache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Everything needed to call the server and to log in again without asking. */
data class StoredLogin(
    val serverUrl: String,
    val username: String,
    /** sha256(password), lowercase hex. The password itself is never stored. */
    val passwordHash: String,
    val token: String,
    val expiresAtMillis: Long,
)

/**
 * Where [Session] keeps its [StoredLogin]. Implemented by `data/Settings`
 * (DataStore); tests use an in-memory fake.
 */
interface CredentialStore {
    suspend fun load(): StoredLogin?
    suspend fun save(login: StoredLogin)
    suspend fun clear()
}

/** What the UI needs to know to choose between the auth screen and the app. */
sealed interface SessionState {
    /** Stored credentials not read yet (first few ms after launch). */
    data object Unknown : SessionState

    /** Show the auth screen, prefilled with these (empty after log out). */
    data class LoggedOut(val serverUrl: String, val username: String) : SessionState

    data class LoggedIn(val serverUrl: String, val username: String) : SessionState
}

/**
 * The session token and its upkeep. Owns the rule "the user logs in once":
 * a dead token is replaced by a silent re-handshake with the stored password
 * hash, and the failed call is retried once. Only a rejected re-handshake
 * (password changed) sends the user back to the auth screen.
 *
 * Being logged in is a fact about stored credentials, not about the network,
 * so a restart lands past the auth screen even offline.
 */
class Session(
    private val client: AmpacheClient,
    private val store: CredentialStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile private var login: StoredLogin? = null
    private val restored = CompletableDeferred<Unit>()

    /** Serialises re-handshakes so ten failing calls cause one handshake, not ten. */
    private val reauthLock = Mutex()

    @Volatile private var lastPingCheckMillis = 0L

    /** Reads stored credentials. Called once at startup; no network. */
    suspend fun restore() {
        val stored = store.load()
        login = stored
        _state.value = if (stored != null) {
            SessionState.LoggedIn(stored.serverUrl, stored.username)
        } else {
            SessionState.LoggedOut("", "")
        }
        restored.complete(Unit)
    }

    /** Auth screen's Connect. Throws [AmpacheException] with a user-facing message. */
    suspend fun login(serverInput: String, username: String, password: String) {
        restored.await()
        val server = AmpacheUrls.normalizeServer(serverInput)
            ?: throw AmpacheException.BadAddress()
        val passwordHash = AmpacheClient.sha256Hex(password)
        val handshake = client.handshake(server, username, passwordHash)
        val fresh = StoredLogin(server, username, passwordHash, handshake.auth, parseExpiry(handshake.sessionExpire))
        store.save(fresh)
        login = fresh
        _state.value = SessionState.LoggedIn(server, username)
    }

    suspend fun logout() {
        restored.await()
        store.clear()
        login = null
        _state.value = SessionState.LoggedOut("", "")
    }

    /**
     * Runs [block] with the current server and token. On [AmpacheException.SessionExpired]
     * re-handshakes and runs it once more; a second expiry is thrown to the caller.
     */
    suspend fun <T> call(block: suspend (server: String, token: String) -> T): T {
        restored.await()
        val before = login ?: throw AmpacheException.SessionExpired()
        return try {
            block(before.serverUrl, before.token)
        } catch (e: AmpacheException.SessionExpired) {
            val after = reauthenticate(before.token)
            block(after.serverUrl, after.token)
        }
    }

    /**
     * Foreground keep-alive (docs/AMPACHE.md → Keep-alive): at most one check a
     * minute, and a ping only when under 20 minutes of session remain. Network
     * failures are ignored — the next real call will re-handshake if needed.
     */
    suspend fun pingIfStale() {
        restored.await()
        val current = login ?: return
        val now = clock()
        if (!shouldPing(now, current.expiresAtMillis, lastPingCheckMillis)) return
        lastPingCheckMillis = now
        try {
            val ping = call { server, token -> client.ping(server, token) }
            val latest = login ?: return
            val expiry = ping.sessionExpire ?: return
            val updated = latest.copy(expiresAtMillis = parseExpiry(expiry))
            store.save(updated)
            login = updated
        } catch (e: AmpacheException) {
            // Unreachable now; nothing to do until the next call.
        }
    }

    /** The token for building stream URLs at request time (SongUriResolver, task 04). */
    fun currentLogin(): StoredLogin? = login

    private suspend fun reauthenticate(deadToken: String): StoredLogin = reauthLock.withLock {
        val current = login ?: throw AmpacheException.SessionExpired()
        // Another caller already replaced the token while we waited for the lock.
        if (current.token != deadToken) return current

        val handshake = try {
            client.handshake(current.serverUrl, current.username, current.passwordHash)
        } catch (e: AmpacheException.ServerError) {
            // The stored hash was rejected: the password changed on the server.
            // Keep server and username so the auth screen is prefilled.
            store.clear()
            login = null
            _state.value = SessionState.LoggedOut(current.serverUrl, current.username)
            throw e
        }
        val fresh = current.copy(token = handshake.auth, expiresAtMillis = parseExpiry(handshake.sessionExpire))
        store.save(fresh)
        login = fresh
        fresh
    }

    companion object {
        const val PING_WHEN_REMAINING_UNDER_MILLIS = 20 * 60 * 1000L
        const val PING_CHECK_THROTTLE_MILLIS = 60 * 1000L

        fun shouldPing(nowMillis: Long, expiresAtMillis: Long, lastCheckMillis: Long): Boolean {
            if (nowMillis - lastCheckMillis < PING_CHECK_THROTTLE_MILLIS) return false
            return expiresAtMillis - nowMillis < PING_WHEN_REMAINING_UNDER_MILLIS
        }

        /** `session_expire` is ISO 8601 with an offset, e.g. `2026-10-12T15:04:05+00:00`. */
        fun parseExpiry(sessionExpire: String): Long =
            try {
                OffsetDateTime.parse(sessionExpire).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                throw AmpacheException.NotJson("unreadable session_expire '$sessionExpire'")
            }
    }
}
