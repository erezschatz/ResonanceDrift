package net.erezschatz.resonancedrift

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader

/**
 * Application entry point. Its only job is to build [AppGraph] once; everything
 * else (the Activity, the playback service, ViewModels) reaches shared objects
 * through [graph].
 */
class ResonanceDriftApp : Application(), SingletonImageLoader.Factory {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }

    /** Every AsyncImage uses this loader: the shared OkHttp client (task 06). */
    override fun newImageLoader(context: PlatformContext): ImageLoader = graph.imageLoader
}
