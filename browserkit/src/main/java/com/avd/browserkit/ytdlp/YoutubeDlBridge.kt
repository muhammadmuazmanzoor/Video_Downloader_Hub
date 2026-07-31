package com.avd.browserkit.ytdlp

import android.app.Application
import android.content.Context
import android.util.Log

object YoutubeDlBridge {
    private const val TAG = "YoutubeDlBridge"
    private const val IMPL_CLASS =
        "com.example.ammar.youtubedldynamic.YoutubeDlEngineImpl"

    @Volatile
    private var engine: YoutubeDlEngine? = null

    fun isReady(): Boolean = engine != null

    fun engineOrNull(): YoutubeDlEngine? = engine

    fun tryLoad(context: Context): Boolean {
        engine?.let { return true }
        return runCatching {
            val app = context.applicationContext as Application
            val clazz = Class.forName(IMPL_CLASS)
            val instance = clazz.getDeclaredConstructor().newInstance() as YoutubeDlEngine
            instance.init(app)
            engine = instance
            Log.d(TAG, "YoutubeDlEngine loaded")
            true
        }.getOrElse {
            Log.d(TAG, "YoutubeDlEngine not available yet: ${it.message}")
            false
        }
    }

    fun clear() {
        engine = null
    }
}
