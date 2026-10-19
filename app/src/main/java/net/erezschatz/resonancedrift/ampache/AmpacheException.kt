package net.erezschatz.resonancedrift.ampache

/**
 * Every way a server call can fail, as one closed set so callers can `when`
 * over it. Two texts per failure: [message] is the technical detail (for logs
 * and diagnostics), [userMessage] is the plain sentence the screen shows.
 */
sealed class AmpacheException(
    message: String,
    val userMessage: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** What the user typed is not a URL even after normalising. */
    class BadAddress :
        AmpacheException("unparseable server address", "That doesn't look like a server address.")

    /** No HTTP answer at all: DNS, refused connection, timeout, TLS. */
    class Unreachable(cause: Throwable) : AmpacheException(
        "unreachable: ${cause.message ?: cause.javaClass.simpleName}",
        "Can't reach the server. Check the address and your connection.",
        cause,
    )

    /** Android blocks plain http:// unless the manifest allows cleartext. */
    class CleartextBlocked(cause: Throwable) : AmpacheException(
        "cleartext blocked: ${cause.message}",
        "Use the server's https:// address.",
        cause,
    )

    /** A non-2xx status other than 401/403 (those are [SessionExpired]). */
    class HttpStatus(val code: Int) :
        AmpacheException("HTTP $code", "That address doesn't look like an Ampache server.")

    /** HTML login pages from proxies, wrong paths, or a body that won't parse. */
    class NotJson(detail: String) :
        AmpacheException("not Ampache JSON: $detail", "That address doesn't look like an Ampache server.")

    /**
     * The error envelope: `{ "error": { "errorCode", "errorMessage" } }`.
     * 4701 is Ampache's code for a rejected handshake.
     */
    class ServerError(val code: String, serverMessage: String) : AmpacheException(
        "server error $code: $serverMessage",
        if (code == "4701") "Wrong username or password." else "The server refused the request.",
    )

    /** Dead token: "Session Expired" envelope, HTTP 401/403, or a ping without `session_expire`. */
    class SessionExpired : AmpacheException("session expired", "Your session expired. Log in again.")
}
