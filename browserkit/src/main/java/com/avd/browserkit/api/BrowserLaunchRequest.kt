package com.avd.browserkit.api

enum class BrowserLaunchMode {
    SEARCH,
    URL,
    BLANK,
}

data class BrowserLaunchRequest(
    val mode: BrowserLaunchMode = BrowserLaunchMode.BLANK,
    val query: String? = null,
    val url: String? = null,
)

data class BrowserKitConfig(
    val searchUrlTemplate: String = DEFAULT_SEARCH_TEMPLATE,
    val mobileUserAgent: String = DEFAULT_MOBILE_UA,
    val desktopUserAgent: String = DEFAULT_DESKTOP_UA,
    val useDesktopMode: Boolean = false,
    val enableAdblock: Boolean = false,
    val enableProxy: Boolean = false,
) {
    companion object {
        const val DEFAULT_SEARCH_TEMPLATE = "https://www.google.com/search?q={query}"
        const val DEFAULT_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        // Windows Chrome — FB treats Linux X11 UA almost like mobile; layout won't change.
        const val DEFAULT_DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
