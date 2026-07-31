package com.avd.browserkit.util

import android.util.Log

/**
 * Logcat filters (either works):
 * - tag [AVD_Flow]  → all browserkit flow logs
 * - tag [DM] / [YtDlp] / [Detect] → step-only (same messages)
 *
 * Format on AVD_Flow: `[Step] message`
 */
object BrowserKitLog {
    const val TAG = "AVD_Flow"

    fun d(step: String, message: String) {
        val line = "[$step] $message"
        Log.d(TAG, line)
        Log.d(stepTag(step), message)
    }

    fun i(step: String, message: String) {
        val line = "[$step] $message"
        Log.i(TAG, line)
        Log.i(stepTag(step), message)
    }

    fun w(step: String, message: String, t: Throwable? = null) {
        val line = "[$step] $message"
        if (t != null) {
            Log.w(TAG, line, t)
            Log.w(stepTag(step), message, t)
        } else {
            Log.w(TAG, line)
            Log.w(stepTag(step), message)
        }
    }

    fun e(step: String, message: String, t: Throwable? = null) {
        val line = "[$step] $message"
        if (t != null) {
            Log.e(TAG, line, t)
            Log.e(stepTag(step), message, t)
        } else {
            Log.e(TAG, line)
            Log.e(stepTag(step), message)
        }
    }

    fun shortUrl(url: String?, max: Int = 160): String {
        if (url.isNullOrBlank()) return "(empty)"
        return if (url.length <= max) url else url.take(max) + "…"
    }

    /** Android tag max ~23 chars on older APIs. */
    private fun stepTag(step: String): String {
        val cleaned = step.trim().ifBlank { "Step" }
        return if (cleaned.length <= 23) cleaned else cleaned.take(23)
    }
}
