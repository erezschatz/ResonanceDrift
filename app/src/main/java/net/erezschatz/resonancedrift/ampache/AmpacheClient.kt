package net.erezschatz.resonancedrift.ampache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.erezschatz.resonancedrift.ampache.model.Handshake
import net.erezschatz.resonancedrift.ampache.model.Ping
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.UnknownServiceException
import java.security.MessageDigest

/**
 * The Ampache JSON API, stateless: every call takes the server and token it
 * should use. Holding the token, and re-authenticating when it dies, is
 * [Session]'s job. See docs/AMPACHE.md for the contract this follows.
 */
class AmpacheClient(
    private val http: OkHttpClient,
    private val json: Json,
) {

    /**
     * `auth = sha256(username + passwordHash)`, version 6.0.0, no timestamp —
     * the form ReDrift used against the real server.
     */
    suspend fun handshake(server: String, username: String, passwordHash: String): Handshake {
        val auth = sha256Hex(username + passwordHash)
        val url = AmpacheUrls.api(server, "handshake", auth, listOf("version" to API_VERSION))
        val body = get(url)
        val handshake = decode(Handshake.serializer(), body)
        if (handshake.auth.isEmpty() || handshake.sessionExpire.isEmpty()) {
            throw AmpacheException.NotJson("handshake without auth or session_expire")
        }
        return handshake
    }

    suspend fun ping(server: String, token: String): Ping {
        val body = get(AmpacheUrls.api(server, "ping", token))
        val ping = decode(Ping.serializer(), body)
        // A dead token still gets a 200 ping, just without session_expire.
        if (ping.sessionExpire.isNullOrEmpty()) throw AmpacheException.SessionExpired()
        return ping
    }

    /**
     * One GET, checked in the order ReDrift checked: transport, HTTP status,
     * Content-Type, JSON object, error envelope. Returns the parsed object.
     */
    private suspend fun get(url: HttpUrl): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        val text: String
        try {
            http.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 403) throw AmpacheException.SessionExpired()
                if (!response.isSuccessful) throw AmpacheException.HttpStatus(response.code)
                val contentType = response.header("Content-Type").orEmpty()
                if (!contentType.contains("application/json")) {
                    throw AmpacheException.NotJson(contentType.ifEmpty { "no Content-Type" })
                }
                text = response.body.string()
            }
        } catch (e: UnknownServiceException) {
            // OkHttp's wording for "CLEARTEXT communication … not permitted".
            throw AmpacheException.CleartextBlocked(e)
        } catch (e: IOException) {
            throw AmpacheException.Unreachable(e)
        }

        val obj = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: SerializationException) {
            throw AmpacheException.NotJson("unparseable body")
        } catch (e: IllegalArgumentException) {
            // jsonObject on an array or primitive
            throw AmpacheException.NotJson("not a JSON object")
        }

        val error = obj["error"]
        if (error != null) throw envelopeToException(error.jsonObject)
        obj
    }

    private fun envelopeToException(error: JsonObject): AmpacheException {
        // errorCode is a string in Ampache 6 JSON and a number in older servers.
        val code = (error["errorCode"] as? JsonPrimitive)?.content.orEmpty()
        val message = (error["errorMessage"] as? JsonPrimitive)?.content ?: "Server error $code"
        if (message.contains("Session Expired", ignoreCase = true)) return AmpacheException.SessionExpired()
        return AmpacheException.ServerError(code, message)
    }

    private fun <T> decode(deserializer: DeserializationStrategy<T>, body: JsonObject): T =
        try {
            json.decodeFromJsonElement(deserializer, body)
        } catch (e: SerializationException) {
            throw AmpacheException.NotJson("unexpected shape: ${e.message}")
        }

    companion object {
        const val API_VERSION = "6.0.0"

        /** Lowercase hex SHA-256 of the UTF-8 bytes, as ReDrift's crypto.subtle did. */
        fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
