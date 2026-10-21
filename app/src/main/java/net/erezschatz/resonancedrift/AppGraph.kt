package net.erezschatz.resonancedrift

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.erezschatz.resonancedrift.ampache.AmpacheClient
import net.erezschatz.resonancedrift.ampache.Session
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import net.erezschatz.resonancedrift.data.Settings
import net.erezschatz.resonancedrift.playback.SongUriResolver
import net.erezschatz.resonancedrift.ui.player.PlayerConnection
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * All process-wide singletons, constructed by hand in dependency order.
 *
 * This is the whole dependency-injection story: no framework, no annotations,
 * no reflection. A class that needs something takes it as a constructor
 * parameter; this file is the one place that knows how everything is wired.
 * Read top to bottom and you have the architecture.
 *
 * Both the Activity and the playback service live in the same process, so one
 * instance serves both. Objects that must be unique per process (Media3's
 * SimpleCache, the Room database) belong here for that reason.
 */
class AppGraph(private val context: Context) {

    val appContext: Context = context.applicationContext

    /**
     * Work that belongs to the process, not to a screen (restoring the session
     * at startup). SupervisorJob: one failed child must not cancel the rest.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Shared HTTP client: Ampache API calls, audio streaming, album art. */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Lenient Ampache JSON; defined beside the client so the fixture tests use the same one. */
    val json: Json = AmpacheClient.json

    /** DataStore requires exactly one instance per file, hence here. */
    val settings: Settings = Settings(
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") },
    )

    val ampache: AmpacheClient = AmpacheClient(httpClient, json)

    val session: Session = Session(ampache, settings)

    init {
        appScope.launch { session.restore() }
    }

    /** Reads the token at request time, so stream URLs survive a re-handshake. */
    val songUriResolver: SongUriResolver = SongUriResolver { session.currentLogin() }

    /**
     * Media3 allows exactly one SimpleCache per directory per process. Lazy:
     * opening it reads its index from disk, which only the playback service
     * needs. In filesDir, not cacheDir — the system may delete cacheDir files
     * under SimpleCache's feet; the LRU evictor bounds the size instead.
     */
    val mediaCache: SimpleCache by lazy {
        SimpleCache(
            File(appContext.filesDir, "media-cache"),
            LeastRecentlyUsedCacheEvictor(MEDIA_CACHE_BYTES),
            StandaloneDatabaseProvider(appContext),
        )
    }

    /** The UI's MediaController; MainActivity connects it while visible. */
    val playerConnection: PlayerConnection = PlayerConnection(appContext)

    // Next, in this order, as they are implemented:
    //   db            (Room)
    //   library       (LibraryRepository over ampache + db)
    //   playbackLog

    private companion object {
        const val MEDIA_CACHE_BYTES = 512L * 1024 * 1024
    }
}

/** Convenience for Activities/Services: `graph` instead of a cast every time. */
val Context.graph: AppGraph
    get() = (applicationContext as ResonanceDriftApp).graph
