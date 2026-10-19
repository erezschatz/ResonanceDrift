package net.erezschatz.resonancedrift.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import net.erezschatz.resonancedrift.ampache.CredentialStore
import net.erezschatz.resonancedrift.ampache.LastAccount
import net.erezschatz.resonancedrift.ampache.StoredLogin

/**
 * DataStore-backed settings. Today that is only the login: server URL,
 * username, password hash (never the password), session token and expiry.
 * Single account (docs/SPEC.md → Data).
 */
class Settings(private val store: DataStore<Preferences>) : CredentialStore {

    override suspend fun load(): StoredLogin? {
        val prefs = store.data.first()
        val server = prefs[SERVER_URL] ?: return null
        val username = prefs[USERNAME] ?: return null
        val passwordHash = prefs[PASSWORD_HASH] ?: return null
        return StoredLogin(
            serverUrl = server,
            username = username,
            passwordHash = passwordHash,
            token = prefs[TOKEN].orEmpty(),
            expiresAtMillis = prefs[EXPIRES_AT] ?: 0L,
        )
    }

    override suspend fun save(login: StoredLogin) {
        store.edit { prefs ->
            prefs[SERVER_URL] = login.serverUrl
            prefs[USERNAME] = login.username
            prefs[PASSWORD_HASH] = login.passwordHash
            prefs[TOKEN] = login.token
            prefs[EXPIRES_AT] = login.expiresAtMillis
        }
    }

    override suspend fun lastAccount(): LastAccount? {
        val prefs = store.data.first()
        val server = prefs[SERVER_URL] ?: return null
        val username = prefs[USERNAME] ?: return null
        return LastAccount(server, username)
    }

    override suspend fun clearSecrets() {
        store.edit { prefs ->
            prefs.remove(PASSWORD_HASH)
            prefs.remove(TOKEN)
            prefs.remove(EXPIRES_AT)
        }
    }

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val TOKEN = stringPreferencesKey("session_token")
        val EXPIRES_AT = longPreferencesKey("session_expires_at")
    }
}
