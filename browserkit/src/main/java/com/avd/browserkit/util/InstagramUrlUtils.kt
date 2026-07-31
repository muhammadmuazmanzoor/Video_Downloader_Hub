package com.avd.browserkit.util

object InstagramUrlUtils {
    private val pagePattern = Regex(
        ".*(instagram\\.com|instagr\\.am).*",
        RegexOption.IGNORE_CASE,
    )

    fun isInstagramUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return pagePattern.matches(url)
    }

    /**
     * Instagram media CDN — do NOT match bare fbcdn (that is Facebook).
     * Typical hosts: *.cdninstagram.com, instagram.*.fbcdn.net, scontent*.cdninstagram.com
     */
    fun isInstagramMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("http", ignoreCase = true)) return false
        if (url.contains("blob:", ignoreCase = true)) return false
        val lower = url.lowercase()
        if (lower.contains("cdninstagram.com")) return true
        if (lower.contains("instagram.") && lower.contains("fbcdn.net")) return true
        // Path patterns used by IG video delivery (not post pages like /reel/)
        if (lower.contains("/o1/v/t16/") || lower.contains("/v/t16/")) {
            return lower.contains("instagram") || lower.contains("cdninstagram") ||
                lower.contains("fbcdn")
        }
        // Direct file on instagram.com host (rare)
        return lower.contains("instagram.com") &&
            (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mpd"))
    }

    const val REFERER = "https://www.instagram.com/"
}
