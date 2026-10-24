package net.erezschatz.resonancedrift.playback

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Times each stream request, for the playback log: how long the server took
 * to answer (response headers) and to send the first audio bytes, and whether
 * a new connection had to be opened. That splits "the song took 2 s to start"
 * into network setup vs the server getting ready. One listener per call.
 * Only the song id from the URL is logged, never the URL (it carries the token).
 */
class StreamTiming(
    private val log: PlaybackLog,
    private val clock: () -> Long = System::nanoTime,
) : EventListener() {

    private var startNanos = 0L
    private var headersMs = -1L
    private var connectMs = -1L
    private var connectStartNanos = 0L

    override fun callStart(call: Call) {
        startNanos = clock()
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        connectStartNanos = clock()
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        connectMs = msSince(connectStartNanos)
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        headersMs = msSince(startNanos)
    }

    override fun responseBodyStart(call: Call) {
        val connection = if (connectMs >= 0) "new connection in $connectMs ms" else "reused connection"
        log.log("stream ${songOf(call)}: answered in $headersMs ms, first bytes at ${msSince(startNanos)} ms ($connection)")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        log.log("stream ${songOf(call)}: failed after ${msSince(startNanos)} ms (${ioe.javaClass.simpleName})")
    }

    private fun msSince(nanos: Long) = (clock() - nanos) / 1_000_000

    companion object {
        /** Times only stream requests (`play/index.php`); art and API calls share the client but not this. */
        fun factory(log: PlaybackLog): EventListener.Factory = EventListener.Factory { call ->
            if (call.request().url.encodedPath.endsWith("/play/index.php")) StreamTiming(log) else EventListener.NONE
        }

        fun songOf(call: Call): String = "song-${call.request().url.queryParameter("oid") ?: "?"}"
    }
}
