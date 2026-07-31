package com.avd.browserkit.util

object FacebookUrlUtils {
    private val facebookPattern =
        Regex(".*(facebook\\.com|fb\\.watch|fbcdn\\.net|fbsbx\\.com).*", RegexOption.IGNORE_CASE)

    fun isFacebookUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return facebookPattern.matches(url)
    }
}
