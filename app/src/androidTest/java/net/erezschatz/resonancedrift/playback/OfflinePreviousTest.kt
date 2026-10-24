package net.erezschatz.resonancedrift.playback

import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.erezschatz.resonancedrift.ampache.StoredLogin
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The step-05 bug (task 11): offline, going back to a song that was fully
 * loaded did not play it. This runs the real player chain (PlayerFactory:
 * resolver → media cache → OkHttp) against a fake server, plays song 1 into
 * song 2, cuts the network, and goes back to song 1.
 */
@RunWith(AndroidJUnit4::class)
class OfflinePreviousTest {

    @get:Rule val folder = TemporaryFolder()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var cache: SimpleCache
    private lateinit var player: ExoPlayer
    private lateinit var log: PlaybackLog

    @Volatile private var online = true
    @Volatile private var sendLength = true
    private val requests = mutableListOf<String>()
    private var cacheReport = ""

    @Before
    fun setUp() {
        thread = HandlerThread("player").apply { start() }
        handler = Handler(thread.looper)
        cache = SimpleCache(folder.newFolder("cache"), NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        log = PlaybackLog(folder.newFolder("log")).apply { startRun("test") }
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> serve(chain) }).build()
        val login = StoredLogin("https://music.test", "erez", "hash", "token", Long.MAX_VALUE)
        onPlayerThread {
            player = PlayerFactory.create(context, http, cache, SongUriResolver { login }, log)
            player.addAnalyticsListener(PlaybackEvents(log))
        }
    }

    @After
    fun tearDown() {
        onPlayerThread { player.release() }
        cache.release()
        thread.quitSafely()
    }

    @Test
    fun previousOfflinePlaysTheCachedSong_withContentLength() = previousOfflinePlaysTheCachedSong()

    @Test
    fun previousOfflinePlaysTheCachedSong_withoutContentLength() {
        sendLength = false
        previousOfflinePlaysTheCachedSong()
    }

    private fun previousOfflinePlaysTheCachedSong() {
        onPlayerThread {
            player.setMediaItems((1L..3L).map { song(it) })
            player.prepare()
            player.play()
        }
        waitFor("song 2 playing") { player.currentMediaItemIndex == 1 && player.isPlaying }

        cacheReport = "cache keys ${cache.keys}, song-1 cached bytes ${cache.getCachedBytes("song-1", 0L, C.LENGTH_UNSET.toLong())}, " +
            "song-1 length ${ContentMetadata.getContentLength(cache.getContentMetadata("song-1"))}"
        online = false
        onPlayerThread { player.seekToPreviousMediaItem() }

        waitFor("song 1 playing again, offline") {
            player.currentMediaItemIndex == 0 && player.isPlaying && player.currentPosition > 500
        }
        var error: PlaybackException? = null
        onPlayerThread { error = player.playerError }
        assertNull(error)
        val log = this.log.entries().joinToString("\n") { it.text }
        assertTrue("song 1 was never requested again over the network:\n$log", requests.count { it == "1" } == 1)
    }

    /**
     * Without ExoPlayer: a read with the progressive loader's flags, from a server
     * that sends no Content-Length, still lands in the cache (the resolver clears
     * FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN).
     */
    @Test
    fun aFullReadThroughTheChainLandsInTheCache() {
        sendLength = false
        val withFlag = readThroughChain(5, DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN or DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
        val withoutFlag = readThroughChain(6, 0)
        val report = "with the loader's flags: $withFlag; without: $withoutFlag; keys ${cache.keys}"
        assertTrue(report, cache.getCachedBytes("song-5", 0L, C.LENGTH_UNSET.toLong()) > 0)
        assertTrue(report, cache.getCachedBytes("song-6", 0L, C.LENGTH_UNSET.toLong()) > 0)
    }

    private fun readThroughChain(songId: Long, flags: Int): String {
        val login = StoredLogin("https://music.test", "erez", "hash", "token", Long.MAX_VALUE)
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> serve(chain) }).build()
        val source = PlayerFactory.dataSourceFactory(http, cache, SongUriResolver { login }, log).createDataSource()
        val spec = DataSpec.Builder().setUri(SongUriResolver.uriFor(songId)).setKey("song-$songId").setFlags(flags).build()
        val opened = source.open(spec)
        val read = DataSourceUtil.readToEnd(source).size
        source.close()
        return "open() said $opened, read $read, cached ${cache.getCachedBytes("song-$songId", 0L, C.LENGTH_UNSET.toLong())}"
    }

    private fun song(id: Long): MediaItem = MediaItems.withPlaybackUri(MediaItem.Builder().setMediaId(id.toString()).build())

    /** Every song is 1.5 s of WAV; offline is an IOException, as in airplane mode. */
    private fun serve(chain: Interceptor.Chain): Response {
        val songId = chain.request().url.queryParameter("oid").orEmpty()
        synchronized(requests) { requests.add(songId) }
        if (!online) throw IOException("offline")
        val bytes = wav(seconds = 1.5)
        val range = chain.request().header("Range")
        val start = range?.removePrefix("bytes=")?.substringBefore("-")?.toIntOrNull() ?: 0
        val slice = bytes.copyOfRange(start, bytes.size)
        val body = if (sendLength) {
            slice.toResponseBody("audio/wav".toMediaType())
        } else {
            UnknownLengthBody(slice)
        }
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(if (start > 0) 206 else 200)
            .message("OK")
            .header("Content-Type", "audio/wav")
            // Media3's OkHttp source reads the length from this header, not from the body.
            .apply { if (sendLength) header("Content-Length", slice.size.toString()) }
            .body(body)
            .build()
    }

    /** A body with no Content-Length, like a server streaming without one. */
    private class UnknownLengthBody(private val bytes: ByteArray) : ResponseBody() {
        override fun contentType() = "audio/wav".toMediaType()
        override fun contentLength() = -1L
        override fun source(): BufferedSource = Buffer().write(bytes)
    }

    private fun wav(seconds: Double): ByteArray {
        val rate = 8000
        val samples = (rate * seconds).toInt()
        val data = samples * 2
        val buffer = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(data)
        for (i in 0 until samples) buffer.putShort((Math.sin(i * 2 * Math.PI * 440 / rate) * 3000).toInt().toShort())
        return buffer.array()
    }

    private fun onPlayerThread(block: () -> Unit) {
        val done = CountDownLatch(1)
        var failure: Throwable? = null
        handler.post {
            try {
                block()
            } catch (e: Throwable) {
                failure = e
            } finally {
                done.countDown()
            }
        }
        assertTrue("player thread stuck", done.await(10, TimeUnit.SECONDS))
        failure?.let { throw it }
    }

    private fun waitFor(what: String, timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var met = false
            onPlayerThread { met = condition() }
            if (met) return
            Thread.sleep(100)
        }
        val log = this.log.entries().joinToString("\n") { it.text }
        throw AssertionError("timed out waiting for $what; requests $requests; $cacheReport\n$log")
    }

}
