package com.avd.browserkit.util

import android.content.Context

/**
 * Super settings ported for detection:
 * - force stream: accept video Content-Type even when Content-Length unknown / below threshold
 * - legacy m3u8: skip Avd parser, keep thin HLS candidate (yt-dlp path)
 */
object BrowserDetectionPrefs {
    private const val PREFS = "browserkit_detection"
    private const val KEY_FORCE_STREAM = "force_stream_detection"
    private const val KEY_LEGACY_M3U8 = "use_legacy_m3u8_detection"

    fun isForceStreamDetection(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORCE_STREAM, false)
    }

    fun setForceStreamDetection(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FORCE_STREAM, enabled).apply()
    }

    fun isUseLegacyM3u8Detection(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LEGACY_M3U8, false)
    }

    fun setUseLegacyM3u8Detection(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LEGACY_M3U8, enabled).apply()
    }
}
