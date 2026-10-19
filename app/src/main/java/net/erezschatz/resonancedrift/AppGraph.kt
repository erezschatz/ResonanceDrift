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
import net.erezschatz.resonancedrift.data.Settings
import okhttp3.OkHttpClient
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

    /**
     * Ampache's JSON is wide and changes between server versions; unknown keys
     * are ignored so a new field on the server never breaks parsing here.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** DataStore requires exactly one instance per file, hence here. */
    val settings: Settings = Settings(
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") },
    )

    val ampache: AmpacheClient = AmpacheClient(httpClient, json)

    val session: Session = Session(ampache, settings)

    init {
        appScope.launch { session.restore() }
    }

    // Next, in this order, as they are implemented:
    //   db            (Room)
    //   library       (LibraryRepository over ampache + db)
    //   mediaCache    (Media3 SimpleCache — exactly one per process)
    //   playbackLog
}

/** Convenience for Activities/Services: `graph` instead of a cast every time. */
val Context.graph: AppGraph
    get() = (applicationContext as ResonanceDriftApp).graph
