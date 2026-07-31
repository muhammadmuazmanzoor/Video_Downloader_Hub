package com.avd.browserkit.util

import android.net.Uri

object YoutubeUrlUtils {
    fun isYouTubeUrl(url: String): Boolean {
        val host = normalizedHost(url) ?: return false
        return host == "youtu.be" ||
            host == "youtube.com" ||
            host.endsWith(".youtube.com") ||
            host == "youtube-nocookie.com" ||
            host.endsWith(".youtube-nocookie.com") ||
            host == "youtubekids.com" ||
            host.endsWith(".youtubekids.com") ||
            host == "m.youtube.com"
    }

    private fun normalizedHost(url: String): String? {
        return runCatching {
            Uri.parse(url).host?.lowercase()?.removePrefix("www.")?.removePrefix("m.")
        }.getOrNull()
    }
}
