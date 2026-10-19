package net.erezschatz.resonancedrift.ampache

/**
 * Every way a server call can fail, as one closed set so callers can `when`
 * over it. [message] is written for the auth screen: it is shown verbatim.
 */
sealed class AmpacheException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** What the user typed is not a URL even after normalising. */
    class BadAddress : AmpacheException("That doesn't look like a server address")

    /** No HTTP answer at all: DNS, refused connection, timeout, TLS. */
    class Unreachable(cause: Throwable) :
        AmpacheException("Can't reach the server: ${cause.message ?: cause.javaClass.simpleName}", cause)

    /** Android blocks plain http:// unless the manifest allows cleartext. */
    class CleartextBlocked(cause: Throwable) :
        AmpacheException("Android blocks plain http:// — use the server's https:// address", cause)

    /** A non-2xx status other than 401/403 (those are [SessionExpired]). */
    class HttpStatus(val code: Int) :
        AmpacheException("The server answered HTTP $code. Is this the Ampache address?")

    /** HTML login pages from proxies, wrong paths, or a body that won't parse. */
    class NotJson(detail: String) :
        AmpacheException("The server didn't answer with Ampache JSON ($detail). Is this the Ampache address?")

    /** The error envelope: `{ "error": { "errorCode", "errorMessage" } }`. */
    class ServerError(val code: String, serverMessage: String) : AmpacheException(serverMessage)

    /** Dead token: "Session Expired" envelope, HTTP 401/403, or a ping without `session_expire`. */
    class SessionExpired : AmpacheException("Session expired")
}
