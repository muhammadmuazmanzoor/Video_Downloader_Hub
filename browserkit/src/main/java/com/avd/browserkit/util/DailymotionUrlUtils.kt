package com.avd.browserkit.util

import android.net.Uri

object DailymotionUrlUtils {
    const val REFERER = "https://www.dailymotion.com/"

    fun isDailymotionUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val host = normalizedHost(url) ?: return false
        return host == "dailymotion.com" ||
            host.endsWith(".dailymotion.com") ||
            host == "dai.ly" ||
            host.endsWith(".dai.ly") ||
            host == "dmcdn.net" ||
            host.endsWith(".dmcdn.net")
    }

    fun isDailymotionMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.contains("dmcdn.net") ||
            lower.contains("dailymotion.com") ||
            lower.contains("dai.ly")
    }

    /** Watch / video page (not bare homepage). */
    fun isDailymotionVideoPage(url: String?): Boolean {
        if (!isDailymotionUrl(url)) return false
        val path = runCatching { Uri.parse(url).path.orEmpty().lowercase() }.getOrDefault("")
        return path.contains("/video/") || path.contains("/playlist/")
    }

    private fun normalizedHost(url: String): String? {
        return runCatching {
            val candidate = if (url.contains("://")) url else "https://$url"
            Uri.parse(candidate).host
                ?.lowercase()
                ?.trimEnd('.')
                ?.removePrefix("www.")
                ?.removePrefix("m.")
                ?.removePrefix("mobile.")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
