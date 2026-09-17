package com.shashanksoni.kharchahogayabhai

import android.app.Application
import android.content.Context
import com.shashanksoni.kharchahogayabhai.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KharchaApplication : Application() {

    lateinit var container: AppContainer
        private set

    /** Outlives any screen; used for startup work that must not be cancelled by navigation. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch { container.prepareLocalData() }
    }
}

/** The app's dependency graph, reachable from any Android context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as KharchaApplication).container
