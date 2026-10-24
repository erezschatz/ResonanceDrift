package net.erezschatz.resonancedrift.ampache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.erezschatz.resonancedrift.ampache.model.Album
import net.erezschatz.resonancedrift.ampache.model.Artist
import net.erezschatz.resonancedrift.ampache.model.Genre
import net.erezschatz.resonancedrift.ampache.model.Handshake
import net.erezschatz.resonancedrift.ampache.model.Ping
import net.erezschatz.resonancedrift.ampache.model.Playlist
import net.erezschatz.resonancedrift.ampache.model.Song
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.UnknownServiceException
import java.security.MessageDigest

/**
 * The Ampache JSON API, stateless: every call takes the server and token it
 * should use. Holding the token, and re-authenticating when it dies, is
 * [Session]'s job, so callers go through it:
 * `session.call { server, token -> client.albumSongs(server, token, id) }`.
 * See docs/AMPACHE.md for the contract this follows; one method per action.
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

    // --- Albums

    /** One page of albums. The Albums screen passes a random [offset] for variety. */
    suspend fun albums(server: String, token: String, offset: Int, limit: Int): List<Album> =
        list(get(AmpacheUrls.api(server, "albums", token, page(offset, limit))), "album", Album.serializer())

    suspend fun album(server: String, token: String, albumId: Long): Album =
        single(get(AmpacheUrls.api(server, "album", token, filter(albumId))), "album", Album.serializer())

    /** Track order as the server gives it. */
    suspend fun albumSongs(server: String, token: String, albumId: Long): List<Song> =
        list(get(AmpacheUrls.api(server, "album_songs", token, filter(albumId))), "song", Song.serializer())

    // --- Artists

    suspend fun artists(server: String, token: String, offset: Int, limit: Int): List<Artist> =
        list(get(AmpacheUrls.api(server, "artists", token, page(offset, limit))), "artist", Artist.serializer())

    suspend fun artist(server: String, token: String, artistId: Long): Artist =
        single(get(AmpacheUrls.api(server, "artist", token, filter(artistId))), "artist", Artist.serializer())

    /** Server order is kept (it is the artist's discography order). */
    suspend fun artistAlbums(server: String, token: String, artistId: Long): List<Album> =
        list(get(AmpacheUrls.api(server, "artist_albums", token, filter(artistId))), "album", Album.serializer())

    /** Unused by the phase-1 UI; kept so the client covers what ReDrift's did. */
    suspend fun artistSongs(server: String, token: String, artistId: Long): List<Song> =
        list(get(AmpacheUrls.api(server, "artist_songs", token, filter(artistId))), "song", Song.serializer())

    // --- Genres

    suspend fun genres(server: String, token: String, offset: Int, limit: Int): List<Genre> =
        list(get(AmpacheUrls.api(server, "genres", token, page(offset, limit))), "genre", Genre.serializer())

    suspend fun genre(server: String, token: String, genreId: Long): Genre =
        single(get(AmpacheUrls.api(server, "genre", token, filter(genreId))), "genre", Genre.serializer())

    /** Server order; Genre detail sorts by year, then name (task 08). */
    suspend fun genreAlbums(server: String, token: String, genreId: Long): List<Album> =
        list(get(AmpacheUrls.api(server, "genre_albums", token, filter(genreId))), "album", Album.serializer())

    /** Unused since Genre detail dropped "Play all" (D30); kept so the client covers what ReDrift's did. */
    suspend fun genreSongs(server: String, token: String, genreId: Long): List<Song> =
        list(get(AmpacheUrls.api(server, "genre_songs", token, filter(genreId))), "song", Song.serializer())

    // --- Playlists

    suspend fun playlists(server: String, token: String, offset: Int, limit: Int): List<Playlist> =
        list(get(AmpacheUrls.api(server, "playlists", token, page(offset, limit))), "playlist", Playlist.serializer())

    suspend fun playlist(server: String, token: String, playlistId: String): Playlist =
        single(
            get(AmpacheUrls.api(server, "playlist", token, listOf("filter" to playlistId))),
            "playlist",
            Playlist.serializer(),
        )

    /** 2000+ rows is normal; one request, no paging (ReDrift did the same). */
    suspend fun playlistSongs(server: String, token: String, playlistId: String): List<Song> =
        list(
            get(AmpacheUrls.api(server, "playlist_songs", token, listOf("filter" to playlistId))),
            "song",
            Song.serializer(),
        )

    /** The app's only write. Success is the absence of an error envelope. */
    suspend fun playlistRemoveSong(server: String, token: String, playlistId: String, songId: Long) {
        get(AmpacheUrls.api(server, "playlist_remove_song", token, listOf("filter" to playlistId, "song" to songId.toString())))
    }

    // --- Songs

    suspend fun song(server: String, token: String, songId: Long): Song =
        single(get(AmpacheUrls.api(server, "song", token, filter(songId))), "song", Song.serializer())

    /** Scrobble, once per song start. Callers fire and forget; failures are only logged. */
    suspend fun recordPlay(server: String, token: String, songId: Long) {
        get(AmpacheUrls.api(server, "record_play", token, listOf("id" to songId.toString(), "client" to CLIENT_NAME)))
    }

    // --- Search (docs/AMPACHE.md → advanced_search; merging is the Search screen's job)

    /** Albums whose [rule] ("title" or "artist") contains [query]. */
    suspend fun searchAlbums(server: String, token: String, rule: String, query: String, limit: Int = SEARCH_LIMIT): List<Album> =
        list(get(AmpacheUrls.api(server, "advanced_search", token, searchParams("album", rule, query, limit))), "album", Album.serializer())

    /** Songs whose title contains [query]. */
    suspend fun searchSongs(server: String, token: String, query: String, limit: Int = SEARCH_LIMIT): List<Song> =
        list(get(AmpacheUrls.api(server, "advanced_search", token, searchParams("song", "title", query, limit))), "song", Song.serializer())

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

    private fun page(offset: Int, limit: Int) = listOf("offset" to offset.toString(), "limit" to limit.toString())

    private fun filter(id: Long) = listOf("filter" to id.toString())

    /** Same parameters, same order as ReDrift's search(); `rule_1_operator=0` means "contains". */
    private fun searchParams(type: String, rule: String, query: String, limit: Int) = listOf(
        "type" to type,
        "operator" to "and",
        "rule_1" to rule,
        "rule_1_operator" to "0",
        "rule_1_input" to query,
        "offset" to "0",
        "limit" to limit.toString(),
    )

    /**
     * A list action's `{ "<key>": [ ... ] }`. A missing key is an empty result;
     * a lone object where a list belongs is treated as a list of one.
     */
    private fun <T> list(body: JsonObject, key: String, serializer: KSerializer<T>): List<T> =
        when (val element = body[key]) {
            is JsonArray -> decode(ListSerializer(serializer), element)
            is JsonObject -> listOf(decode(serializer, element))
            else -> emptyList()
        }

    /**
     * A single-object action. ReDrift saw both `{ "album": {..} }` and
     * `{ "album": [ {..} ] }`; Ampache 6 may also answer with the object itself
     * at the top level. All three are accepted.
     */
    private fun <T> single(body: JsonObject, key: String, serializer: KSerializer<T>): T =
        when (val element = body[key]) {
            is JsonArray -> {
                val first = element.firstOrNull() ?: throw AmpacheException.NotJson("empty '$key' list")
                decode(serializer, first)
            }
            is JsonObject -> decode(serializer, element)
            else -> decode(serializer, body)
        }

    private fun <T> decode(deserializer: DeserializationStrategy<T>, element: JsonElement): T =
        try {
            json.decodeFromJsonElement(deserializer, element)
        } catch (e: SerializationException) {
            throw AmpacheException.NotJson("unexpected shape: ${e.message}")
        } catch (e: IllegalArgumentException) {
            // LenientLongSerializer on a non-numeric id
            throw AmpacheException.NotJson("unexpected shape: ${e.message}")
        }

    companion object {
        const val API_VERSION = "6.0.0"

        /** Server-side cap per request; a larger `limit` is silently cut to this. */
        const val MAX_PAGE = 5000

        const val SEARCH_LIMIT = 50

        /** What `record_play` shows as the player in Ampache's history. */
        const val CLIENT_NAME = "ResonanceDrift"

        /** Guard against a server that ignores `offset`: 100 × 5000 rows is far beyond any real library. */
        private const val MAX_PAGES = 100

        /**
         * The one Json configuration for Ampache responses (AppGraph and the
         * fixture tests both use it). Ampache's JSON is wide and changes between
         * server versions: unknown keys are ignored, so a new field never breaks
         * parsing; lenient, so a number sent where a string is expected (or the
         * reverse) still reads; nulls coerced to the field's default.
         */
        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        /**
         * Fetches every page of a list action: [MAX_PAGE] at a time until a short
         * page comes back. Each page goes through [fetchPage] separately, so a
         * caller wrapping it in Session.call re-authenticates per page, not per pool.
         * Used for the album pool (shuffle) — fixes ReDrift's 5000-album ceiling.
         */
        suspend fun <T> fetchAllPages(
            pageSize: Int = MAX_PAGE,
            fetchPage: suspend (offset: Int, limit: Int) -> List<T>,
        ): List<T> {
            val all = mutableListOf<T>()
            for (pageNumber in 0 until MAX_PAGES) {
                val page = fetchPage(pageNumber * pageSize, pageSize)
                all.addAll(page)
                if (page.size < pageSize) break
            }
            return all
        }

        /** Lowercase hex SHA-256 of the UTF-8 bytes, as ReDrift's crypto.subtle did. */
        fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
