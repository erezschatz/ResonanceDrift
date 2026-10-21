package net.erezschatz.resonancedrift.ampache

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Every URL the app sends to the server is built here, so the exact strings
 * can be pinned in unit tests. See docs/AMPACHE.md → Base URLs.
 */
object AmpacheUrls {

    /**
     * Turns what the user typed into the `<server>` base: all whitespace removed
     * (a URL has none; keyboards insert it after "."), trailing slashes removed,
     * `https://` assumed when no scheme was given. Returns null when the result
     * is still not a URL.
     */
    fun normalizeServer(input: String): String? {
        var server = input.filterNot { it.isWhitespace() }.trimEnd('/')
        if (server.isEmpty()) return null
        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            server = "https://$server"
        }
        return if (server.toHttpUrlOrNull() == null) null else server
    }

    /**
     * `<server>/server/json.server.php?action=<action>&auth=<auth>&<params>`.
     * Parameter order matches ReDrift: action, auth, then the rest as given.
     */
    fun api(server: String, action: String, auth: String, params: List<Pair<String, String>> = emptyList()): HttpUrl {
        val base = server.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Not a URL: $server")
        val builder = base.newBuilder()
            .addPathSegments("server/json.server.php")
            .addQueryParameter("action", action)
            .addQueryParameter("auth", auth)
        for ((name, value) in params) {
            builder.addQueryParameter(name, value)
        }
        return builder.build()
    }

    /**
     * `<server>/play/index.php?ssid=<token>&type=song&oid=<songId>&format=raw`.
     * Concatenated, not built, exactly as ReDrift's getStreamUrl() did: the
     * token is hex and the id a number, so nothing needs encoding. `format=raw`
     * asks for the original file — no server-side transcoding.
     */
    fun stream(server: String, token: String, songId: Long): String =
        "$server/play/index.php?ssid=$token&type=song&oid=$songId&format=raw"

    /**
     * Album art through the API's `get_art` action, so it carries `auth` like
     * any other call. Same parameters and order as ReDrift's getArtUrl().
     */
    fun art(server: String, token: String, albumId: Long, sizePx: Int = DEFAULT_ART_SIZE_PX): String =
        api(
            server,
            "get_art",
            token,
            listOf("id" to albumId.toString(), "type" to "album", "size" to sizePx.toString()),
        ).toString()

    /** ReDrift's default; grids and the mini player. Now Playing asks for more. */
    const val DEFAULT_ART_SIZE_PX = 300
}
