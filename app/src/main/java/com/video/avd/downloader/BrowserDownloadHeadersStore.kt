package com.video.avd.downloader

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object BrowserDownloadHeadersStore {
    private const val PREF = "browser_host_dl_headers"
    private val gson = Gson()

    fun save(context: Context, taskId: String, headers: Map<String, String>) {
        if (headers.isEmpty()) return
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(taskId, gson.toJson(headers))
            .apply()
    }

    fun load(context: Context, taskId: String): Map<String, String> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(taskId, null)
            ?: return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(raw, type).orEmpty()
        }.getOrDefault(emptyMap())
    }

    fun clear(context: Context, taskId: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(taskId)
            .apply()
    }
}
