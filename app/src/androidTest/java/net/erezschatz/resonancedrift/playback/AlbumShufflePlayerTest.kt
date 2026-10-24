package net.erezschatz.resonancedrift.playback

import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import net.erezschatz.resonancedrift.ampache.StoredLogin
import net.erezschatz.resonancedrift.ampache.model.Ref
import net.erezschatz.resonancedrift.ampache.model.Song
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * AlbumShuffle on the real player (task 12): one-song albums of 1.5 s each,
 * so three album boundaries pass in a few seconds with no UI and no help.
 */
@RunWith(AndroidJUnit4::class)
class AlbumShufflePlayerTest {

    @get:Rule val folder = TemporaryFolder()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var scope: CoroutineScope
    private lateinit var cache: SimpleCache
    private lateinit var player: ExoPlayer
    private lateinit var shuffle: AlbumShuffle
    private lateinit var log: PlaybackLog
    private val history = mutableListOf<Long>()
    private val login = StoredLogin("https://music.test", "erez", "hash", "token", Long.MAX_VALUE)

    /** Albums 1..20; song id = album id × 10. */
    private val library = object : ShuffleLibrary {
        override suspend fun albumPool() = (1L..20L).toList()
        override suspend fun albumSongs(albumId: Long): List<MediaItem> =
            listOf(MediaItems.fromSong(Song(id = albumId * 10, title = "Song of $albumId", album = Ref(albumId, "Album $albumId")), login))
        override suspend fun recentAlbumIds() = synchronized(history) { history.toList() }
        override suspend fun recordStarted(albumId: Long) {
            synchronized(history) { history += albumId }
        }
    }

    @Before
    fun setUp() {
        thread = HandlerThread("player").apply { start() }
        handler = Handler(thread.looper)
        scope = CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher())
        cache = SimpleCache(folder.newFolder("cache"), NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        log = PlaybackLog(folder.newFolder("log")).apply { startRun("test") }
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> serve(chain) }).build()
        onPlayerThread {
            player = PlayerFactory.create(context, http, cache, SongUriResolver { login }, log)
            player.addAnalyticsListener(PlaybackEvents(log))
            shuffle = AlbumShuffle(PlayerQueueController(player), library, log, scope, onStateChanged = { _, _ -> })
            player.addListener(shuffle)
        }
    }

    @After
    fun tearDown() {
        onPlayerThread {
            scope.cancel()
            player.release()
        }
        cache.release()
        thread.quitSafely()
    }

    @Test
    fun threeAlbumBoundariesPassOnTheirOwn() {
        onPlayerThread { shuffle.start(albumId = 1) }

        waitFor("three albums after the first") { synchronized(history) { history.size } >= 4 }

        var state = 0
        onPlayerThread { state = player.playbackState }
        val started = synchronized(history) { history.toList() }
        assertTrue("never ended: state $state", state != Player.STATE_ENDED)
        assertTrue("no album twice: $started", started.distinct().size == started.size)
    }

    private fun serve(chain: Interceptor.Chain): Response {
        val bytes = wav(seconds = 1.5)
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Type", "audio/wav")
            .body(bytes.toResponseBody("audio/wav".toMediaType()))
            .build()
    }

    private fun wav(seconds: Double): ByteArray {
        val rate = 8000
        val samples = (rate * seconds).toInt()
        val data = samples * 2
        val buffer = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(data)
        for (i in 0 until samples) buffer.putShort((Math.sin(i * 2 * Math.PI * 330 / rate) * 3000).toInt().toShort())
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

    private fun waitFor(what: String, timeoutMs: Long = 20_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(100)
        }
        val log = this.log.entries().joinToString("\n") { it.text }
        throw AssertionError("timed out waiting for $what; started ${synchronized(history) { history }}\n$log")
    }
}
