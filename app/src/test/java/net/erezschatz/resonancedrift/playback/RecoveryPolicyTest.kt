package net.erezschatz.resonancedrift.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import net.erezschatz.resonancedrift.playback.RecoveryPolicy.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPolicyTest {

    // --- layer 1: the loader

    @Test
    fun `network failures retry with backoff capped at 30 s`() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (1..7).map { RecoveryPolicy.loadRetryDelayMs(httpStatus = null, errorCount = it) })
    }

    @Test
    fun `server errors and throttling retry`() {
        assertEquals(1_000L, RecoveryPolicy.loadRetryDelayMs(500, 1))
        assertEquals(1_000L, RecoveryPolicy.loadRetryDelayMs(503, 1))
        assertEquals(1_000L, RecoveryPolicy.loadRetryDelayMs(408, 1))
        assertEquals(1_000L, RecoveryPolicy.loadRetryDelayMs(429, 1))
    }

    @Test
    fun `client errors do not retry`() {
        listOf(400, 401, 403, 404, 416).forEach {
            assertEquals("HTTP $it", C.TIME_UNSET, RecoveryPolicy.loadRetryDelayMs(it, 1))
        }
    }

    @Test
    fun `the loader keeps trying for about ten minutes`() {
        val total = (1..RecoveryPolicy.LOAD_RETRIES).sumOf { RecoveryPolicy.loadRetryDelayMs(null, it) }
        assertTrue("was ${total / 1000} s", total in 9 * 60_000L..12 * 60_000L)
    }

    // --- layer 2: NetworkRecovery

    @Test
    fun `recovery backoff is 5, 10, 20, 40, then 60 s`() {
        assertEquals(listOf(5_000L, 10_000L, 20_000L, 40_000L, 60_000L, 60_000L),
            (0..5).map { RecoveryPolicy.recoveryDelayMs(it) })
    }

    @Test
    fun `recovery keeps trying for about half an hour`() {
        val total = (0 until RecoveryPolicy.RECOVERY_ATTEMPTS).sumOf { RecoveryPolicy.recoveryDelayMs(it) }
        assertTrue("was ${total / 60_000} min", total in 25 * 60_000L..35 * 60_000L)
    }

    @Test
    fun `network-class errors are retried`() {
        listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT,
        ).forEach { assertEquals(Action.RETRY, RecoveryPolicy.classify(it, null, reauthTried = false)) }
        assertEquals(Action.RETRY, RecoveryPolicy.classify(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 502, false))
    }

    @Test
    fun `401 and 403 renew the token first, then fall back to retrying`() {
        val bad = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        assertEquals(Action.REAUTH, RecoveryPolicy.classify(bad, 401, reauthTried = false))
        assertEquals(Action.REAUTH, RecoveryPolicy.classify(bad, 403, reauthTried = false))
        assertEquals(Action.RETRY, RecoveryPolicy.classify(bad, 403, reauthTried = true))
    }

    @Test
    fun `an HTML page instead of audio gets one token renewal, then stops`() {
        val html = PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        assertEquals(Action.REAUTH, RecoveryPolicy.classify(html, null, reauthTried = false))
        assertEquals(Action.GIVE_UP, RecoveryPolicy.classify(html, null, reauthTried = true))
    }

    @Test
    fun `missing files and decoder failures are not retried`() {
        assertEquals(Action.GIVE_UP, RecoveryPolicy.classify(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 404, false))
        assertEquals(Action.GIVE_UP, RecoveryPolicy.classify(PlaybackException.ERROR_CODE_DECODING_FAILED, null, false))
    }

    @Test
    fun `no http status when no bad response is in the cause chain`() {
        val plain = RuntimeException("outer", IllegalStateException("inner"))
        assertEquals(null, RecoveryPolicy.httpStatusOf(plain))
    }
}
