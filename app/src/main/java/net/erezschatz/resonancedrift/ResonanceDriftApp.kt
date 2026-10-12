package net.erezschatz.resonancedrift

import android.app.Application

/**
 * Application entry point. Its only job is to build [AppGraph] once; everything
 * else (the Activity, the playback service, ViewModels) reaches shared objects
 * through [graph].
 */
class ResonanceDriftApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
