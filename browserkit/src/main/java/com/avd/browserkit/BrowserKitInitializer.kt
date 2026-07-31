package com.avd.browserkit

import android.content.Context
import com.avd.browserkit.ytdlp.YoutubeDlBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object BrowserKitInitializer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initMutex = Mutex()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        scope.launch { initializeAwait(context.applicationContext) }
    }

    suspend fun initializeAwait(context: Context) {
        if (initialized && YoutubeDlBridge.isReady()) return
        initMutex.withLock {
            if (initialized && YoutubeDlBridge.isReady()) return
            withContext(Dispatchers.IO) {
                initialized = YoutubeDlBridge.tryLoad(context.applicationContext)
            }
        }
    }

    fun initializeBlocking(context: Context) {
        if (initialized && YoutubeDlBridge.isReady()) return
        runBlocking { initializeAwait(context.applicationContext) }
    }

    fun isInitialized(): Boolean = initialized && YoutubeDlBridge.isReady()
}
