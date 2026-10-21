package net.erezschatz.resonancedrift.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Every "should we try again, and when" in playback, as plain functions so
 * they are unit-tested without a player. Two layers use it:
 *  1. ExoPlayer's loader ([RetryingLoadErrorPolicy]): while a load keeps
 *     failing, retry it instead of failing the item. The player sits in
 *     BUFFERING — still "playing" to Android, so the service stays foreground.
 *  2. [NetworkRecovery]: when the player did give up (STATE_IDLE + error).
 */
object RecoveryPolicy {

    /** What NetworkRecovery does with a player error. Never "skip to the next item". */
    enum class Action { RETRY, REAUTH, GIVE_UP }

    /** Loader retries without progress before the player reports the error: ≈10 minutes by [loadRetryDelayMs]. */
    const val LOAD_RETRIES = 24

    /** NetworkRecovery's attempts after the player gave up: ≈30 minutes by [recoveryDelayMs]. */
    const val RECOVERY_ATTEMPTS = 32

    /**
     * Loader backoff: 1, 2, 4, 8, 16, then 30 s. [errorCount] starts at 1 and
     * resets whenever the load made progress. A 4xx other than 408/429 will not
     * get better by waiting (401/403 means the token died: NetworkRecovery's job).
     */
    fun loadRetryDelayMs(httpStatus: Int?, errorCount: Int): Long {
        if (httpStatus != null && httpStatus in 400..499 && httpStatus != 408 && httpStatus != 429) return C.TIME_UNSET
        val exponent = (errorCount - 1).coerceIn(0, 5)
        return minOf(1_000L shl exponent, 30_000L)
    }

    /** NetworkRecovery backoff: 5, 10, 20, 40, then 60 s. [attempt] starts at 0. */
    fun recoveryDelayMs(attempt: Int): Long = minOf(5_000L shl attempt.coerceIn(0, 4), 60_000L)

    /**
     * [reauthTried]: a re-handshake was already done for this failure streak.
     * An HTML page or wrong content type where audio belongs is how an expired
     * stream token can also look (docs/AMPACHE.md → Expiry), so those get one
     * re-handshake before being given up on.
     */
    fun classify(errorCode: Int, httpStatus: Int?, reauthTried: Boolean): Action {
        if (httpStatus == 401 || httpStatus == 403) return if (reauthTried) Action.RETRY else Action.REAUTH
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT,
            -> Action.RETRY

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                if (httpStatus == null || httpStatus >= 500 || httpStatus == 408 || httpStatus == 429) Action.RETRY else Action.GIVE_UP

            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            -> if (reauthTried) Action.GIVE_UP else Action.REAUTH

            else -> Action.GIVE_UP
        }
    }

    /** The HTTP status anywhere in the cause chain, if the failure was a bad response. */
    fun httpStatusOf(error: Throwable): Int? {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) return cause.responseCode
            cause = cause.cause
        }
        return null
    }
}

/** Layer 1: keeps a failing load retrying for ~10 minutes instead of failing the item. */
class RetryingLoadErrorPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val status = RecoveryPolicy.httpStatusOf(loadErrorInfo.exception)
        // Media3's own list of non-retriable failures (parse errors, cleartext,
        // reading past the end, file not found) stays non-retriable.
        if (status == null && super.getRetryDelayMsFor(loadErrorInfo) == C.TIME_UNSET) return C.TIME_UNSET
        return RecoveryPolicy.loadRetryDelayMs(status, loadErrorInfo.errorCount)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = RecoveryPolicy.LOAD_RETRIES
}
