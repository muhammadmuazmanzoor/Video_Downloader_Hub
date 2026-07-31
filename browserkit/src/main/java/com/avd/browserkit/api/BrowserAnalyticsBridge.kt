package com.avd.browserkit.api

/**
 * Host app implements this (Firebase, etc.). BrowserKit never depends on Firebase.
 */
interface BrowserAnalyticsBridge {
    /** Browser activity/host opened. */
    fun onBrowserOpen(mode: String, entryUrl: String)

    /** User landed on a page (new host). */
    fun onBrowserSiteView(site: String, pageUrl: String)

    /** Download enqueued from browser. */
    fun onBrowserDownloadStart(site: String, pageUrl: String, method: String)

    fun onBrowserDownloadSuccess(site: String, pageUrl: String)

    fun onBrowserDownloadFailed(site: String, pageUrl: String, reason: String)
}
