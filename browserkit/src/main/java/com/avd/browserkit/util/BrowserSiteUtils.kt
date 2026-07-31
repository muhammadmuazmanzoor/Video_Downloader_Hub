package com.avd.browserkit.util

import android.net.Uri

object BrowserSiteUtils {
    /** Friendly site label for analytics (facebook, tiktok, host, …). */
    fun siteNameFromUrl(url: String?): String {
        if (url.isNullOrBlank()) return "unknown"
        val host = runCatching {
            Uri.parse(url.trim()).host?.lowercase()?.removePrefix("www.")
        }.getOrNull().orEmpty()
        if (host.isBlank()) return "unknown"
        return when {
            host.contains("facebook") || host.contains("fb.watch") ||
                host.contains("fbcdn") || host.contains("fbsbx") -> "facebook"
            host.contains("instagram") || host.contains("cdninstagram") -> "instagram"
            host.contains("tiktok") || host.contains("musical.ly") -> "tiktok"
            host.contains("twitter") || host == "x.com" || host.endsWith(".x.com") ||
                host.contains("twimg") -> "twitter"
            host.contains("youtube") || host.contains("youtu.be") -> "youtube"
            host.contains("vimeo") -> "vimeo"
            host.contains("dailymotion") || host == "dai.ly" ||
                host.endsWith(".dai.ly") || host.contains("dmcdn") -> "dailymotion"
            host.contains("reddit") -> "reddit"
            host.contains("pinterest") -> "pinterest"
            else -> host
        }
    }
}
