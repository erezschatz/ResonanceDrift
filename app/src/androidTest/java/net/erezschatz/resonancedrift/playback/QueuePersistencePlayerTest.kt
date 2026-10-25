package net.erezschatz.resonancedrift.playback

import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.MediaItem
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.erezschatz.resonancedrift.ampache.StoredLogin
import net.erezschatz.resonancedrift.ampache.model.Song
import net.erezschatz.resonancedrift.data.DbTestRule
import net.erezschatz.resonancedrift.data.QueueState
import net.erezschatz.resonancedrift.data.QueueStateStore
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import kotlin.math.abs

/**
 * QueuePersistence on the real player (task 13): a queue saved from one player
 * comes back in a fresh one at the same song and position, paused.
 */
@RunWith(AndroidJUnit4::class)
class QueuePersistencePlayerTest {

    @get:Rule val folder = TemporaryFolder()
    @get:Rule val dbRule = DbTestRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var scope: CoroutineScope
    private lateinit var writeScope: CoroutineScope
    private lateinit var cache: SimpleCache
    private lateinit var http: OkHttpClient
    private lateinit var log: PlaybackLog
    private lateinit var store: QueueStateStore
    private val players = mutableListOf<ExoPlayer>()
    private val login = StoredLogin("https://music.test", "erez", "hash", "token", Long.MAX_VALUE)

    private fun item(songId: Long): MediaItem = MediaItems.fromSong(Song(id = songId, title = "Song $songId"), login)

    @Before
    fun setUp() {
        thread = HandlerThread("player").apply { start() }
        handler = Handler(thread.looper)
        scope = CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher())
        writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        cache = SimpleCache(folder.newFolder("cache"), NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        log = PlaybackLog(folder.newFolder("log")).apply { startRun("test") }
        http = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> serve(chain) }).build()
        store = QueueStateStore(dbRule.db.queueState())
    }

    @After
    fun tearDown() {
        onPlayerThread {
            scope.cancel()
            players.forEach { it.release() }
        }
        writeScope.cancel()
        cache.release()
        thread.quitSafely()
    }

    /** A player with its QueuePersistence, built on the player thread. */
    private fun newPlayer(): Pair<ExoPlayer, QueuePersistence> {
        lateinit var result: Pair<ExoPlayer, QueuePersistence>
        onPlayerThread {
            val player = PlayerFactory.create(context, http, cache, SongUriResolver { login }, log)
            players += player
            val persistence = QueuePersistence(player, store, log, scope, writeScope, shuffleOn = { true })
            player.addListener(persistence)
            result = player to persistence
        }
        return result
    }

    private fun restore(persistence: QueuePersistence): QueueState? {
        var restored: QueueState? = null
        val done = CountDownLatch(1)
        scope.launch {
            restored = persistence.restore { ids -> ids.map { item(it) } }
            done.countDown()
        }
        assertTrue("restore stuck", done.await(10, TimeUnit.SECONDS))
        return restored
    }

    @Test
    fun aSavedQueueComesBackAtItsSongAndPositionPaused() {
        val (first, firstPersistence) = newPlayer()
        assertEquals("nothing saved yet", null, restore(firstPersistence))
        onPlayerThread {
            first.setMediaItems(listOf(item(1), item(2), item(3)), 1, 12_000)
            first.prepare()
            first.play()
        }
        waitFor("playing") { onPlayerThreadValue { first.isPlaying } }
        // Pausing saves.
        onPlayerThread { first.pause() }
        waitFor("saved") { runBlocking { store.load() }?.index == 1 }
        val saved = runBlocking { store.load() }!!
        assertEquals(listOf(1L, 2L, 3L), saved.songIds)
        assertTrue("saved near 12 s: ${saved.positionMs}", saved.positionMs in 12_000..15_000)
        assertTrue(saved.shuffle)

        val (second, secondPersistence) = newPlayer()
        assertEquals(saved, restore(secondPersistence))
        onPlayerThread {
            assertEquals(3, second.mediaItemCount)
            assertEquals(1, second.currentMediaItemIndex)
            assertTrue("restored near ${saved.positionMs}: ${second.currentPosition}", abs(second.currentPosition - saved.positionMs) < 1_000)
            assertFalse("restored paused (D8)", second.playWhenReady)
        }
    }

    @Test
    fun aNewEmptyPlayerDoesNotOverwriteTheSavedQueueBeforeRestoring() {
        val saved = QueueState(listOf(7, 8), index = 1, positionMs = 5_000, shuffle = false)
        runBlocking { store.save(saved) }

        val (_, persistence) = newPlayer()
        // What the player would report before anything is restored.
        onPlayerThread { persistence.onIsPlayingChanged(false) }
        Thread.sleep(300)
        assertEquals(saved, runBlocking { store.load() })
    }

    private fun serve(chain: Interceptor.Chain): Response {
        val bytes = wav(seconds = 30.0)
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

    private fun <T> onPlayerThreadValue(block: () -> T): T {
        var value: T? = null
        onPlayerThread { value = block() }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun waitFor(what: String, timeoutMs: Long = 20_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(100)
        }
        throw AssertionError("timed out waiting for $what\n" + log.entries().joinToString("\n") { it.text })
    }
}
