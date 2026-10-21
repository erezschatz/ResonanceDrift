package net.erezschatz.resonancedrift.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient

/**
 * Builds the one ExoPlayer the service owns. Every setting here is from
 * docs/SPEC.md → Playback; each has its reason beside it.
 */
object PlayerFactory {

    // Buffer well ahead so a dead spot drains the buffer, not the music (D5).
    // Three minutes is most of a song; ExoPlayer reads on into the next item.
    private const val MIN_BUFFER_MS = 120_000
    private const val MAX_BUFFER_MS = 180_000
    // Defaults: start after 2.5 s buffered, resume after a stall at 5 s.
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

    fun create(context: Context, http: OkHttpClient, cache: Cache, resolver: SongUriResolver): ExoPlayer {
        // rd://song/<id> → stream URL (token fresh per request) → cache by song id → network.
        val network = OkHttpDataSource.Factory(http)
        val cached = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(network)
            // A broken cache must never stop playback; fall through to the network.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val resolving = ResolvingDataSource.Factory(cached, resolver)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            // Otherwise the default byte cap (~13 MB) stops a FLAC buffer well short of the target time.
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(resolving)
                    // Retry failing loads for ~10 min instead of failing the item (task 05).
                    .setLoadErrorHandlingPolicy(RetryingLoadErrorPolicy()),
            )
            // true = ExoPlayer requests and handles audio focus (calls, other apps, ducking).
            .setAudioAttributes(audioAttributes, true)
            // Pause when headphones are unplugged or Bluetooth audio disconnects.
            .setHandleAudioBecomingNoisy(true)
            // CPU + Wi-Fi locks while playing with the screen off; released on pause (D13).
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .build()
    }
}
