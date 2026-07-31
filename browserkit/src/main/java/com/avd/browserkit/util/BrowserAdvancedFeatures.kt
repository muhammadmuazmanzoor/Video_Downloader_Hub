package com.avd.browserkit.util

/**
 * Optional parity hooks for upstream adblock/proxy stacks.
 * Disabled by default — host can enable via [com.avd.browserkit.api.BrowserKitConfig].
 */
object BrowserAdvancedFeatures {
    fun isAdblockEnabled(): Boolean = com.avd.browserkit.api.BrowserKit.getConfig().enableAdblock
    fun isProxyEnabled(): Boolean = com.avd.browserkit.api.BrowserKit.getConfig().enableProxy

    fun shouldBlockUrl(url: String): Boolean {
        if (!isAdblockEnabled()) return false
        val lower = url.lowercase()
        return lower.contains("doubleclick.net") ||
            lower.contains("googlesyndication.com") ||
            lower.contains("/ads/") ||
            lower.contains("adservice")
    }
}
