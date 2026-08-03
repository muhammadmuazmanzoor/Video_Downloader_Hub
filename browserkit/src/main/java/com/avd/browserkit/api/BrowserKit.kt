package com.avd.browserkit.api

import android.content.Context
import android.content.Intent
import com.avd.browserkit.BrowserKitActivity
import com.avd.browserkit.download.BrowserDownloadNotifier
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.ui.history.BrowserHistoryActivity

object BrowserKit {
    const val EXTRA_MODE = "browserkit_extra_mode"
    const val EXTRA_QUERY = "browserkit_extra_query"
    const val EXTRA_URL = "browserkit_extra_url"

    /** Open the browser straight into the tabs switcher instead of a page. */
    const val EXTRA_OPEN_TABS = "browserkit_extra_open_tabs"

    @Volatile
    private var config: BrowserKitConfig = BrowserKitConfig()

    @Volatile
    private var downloadBridge: BrowserDownloadBridge? = null

    @Volatile
    private var analyticsBridge: BrowserAnalyticsBridge? = null

    @Volatile
    private var appContext: Context? = null

    fun setConfig(newConfig: BrowserKitConfig) {
        config = newConfig
    }

    fun getConfig(): BrowserKitConfig = config

    fun setDownloadBridge(bridge: BrowserDownloadBridge?) {
        downloadBridge = bridge
    }

    internal fun getDownloadBridge(): BrowserDownloadBridge? = downloadBridge

    fun restartHostDownload(task: BrowserSharedDownloadTask): Boolean {
        return downloadBridge?.restartHostDownload(task) == true
    }

    fun setAnalyticsBridge(bridge: BrowserAnalyticsBridge?) {
        analyticsBridge = bridge
    }

    internal fun analytics(): BrowserAnalyticsBridge? = analyticsBridge

    fun getAppContext(): Context? = appContext

    /**
     * Stores application context for detection prefs. yt-dlp engine is loaded by the
     * app after the on-demand module is installed — never call heavy init from Application.onCreate.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun createIntent(context: Context, request: BrowserLaunchRequest): Intent {
        return Intent(context, BrowserKitActivity::class.java).apply {
            putExtra(EXTRA_MODE, request.mode.name)
            putExtra(EXTRA_QUERY, request.query)
            putExtra(EXTRA_URL, request.url)
        }
    }

    fun launchSearch(context: Context, query: String) {
        context.startActivity(
            createIntent(
                context,
                BrowserLaunchRequest(
                    mode = BrowserLaunchMode.SEARCH,
                    query = query.trim(),
                ),
            ),
        )
    }

    fun launchUrl(context: Context, url: String) {
        context.startActivity(
            createIntent(
                context,
                BrowserLaunchRequest(
                    mode = BrowserLaunchMode.URL,
                    url = url.trim(),
                ),
            ),
        )
    }

    fun launchBlank(context: Context) {
        context.startActivity(
            createIntent(context, BrowserLaunchRequest(mode = BrowserLaunchMode.BLANK)),
        )
    }

    /**
     * Opens the browser showing the tabs switcher (same screen as the browser chrome's
     * tabs button). Reuses the running browser session so the existing tabs are shown.
     */
    fun launchTabs(context: Context) {
        context.startActivity(
            createIntent(context, BrowserLaunchRequest(mode = BrowserLaunchMode.BLANK))
                .putExtra(EXTRA_OPEN_TABS, true),
        )
    }

    /**
     * Opens browsing history as a standalone screen. Tapping an entry loads it in the browser.
     * Callers that want the picked URL back should launch [BrowserHistoryActivity] with an
     * activity-result contract instead.
     */
    fun launchHistory(context: Context) {
        context.startActivity(BrowserHistoryActivity.intent(context, openInBrowser = true))
    }

    fun notifyBridge(snapshot: com.avd.browserkit.download.BrowserDownloadTask) {
        BrowserKitLog.d(
            "Bridge.Notify",
            "update taskId=${snapshot.id} status=${snapshot.status} percent=${snapshot.percent} " +
                "quality=${snapshot.qualityLabel} page=${snapshot.pageUrl}",
        )
        downloadBridge?.onTaskUpdated(
            BrowserDownloadSnapshot(
                taskId = snapshot.id,
                title = snapshot.title,
                pageUrl = snapshot.pageUrl,
                percent = snapshot.percent,
                status = snapshot.status,
                filePath = snapshot.filePath,
                qualityLabel = snapshot.qualityLabel,
            ),
        )
    }

    fun notifyBridgeCompleted(task: com.avd.browserkit.download.BrowserDownloadTask) {
        BrowserKitLog.i(
            "Bridge.Notify",
            "complete taskId=${task.id} success=${task.status == com.avd.browserkit.download.BrowserDownloadStatus.COMPLETED} " +
                "path=${task.filePath}",
        )
        downloadBridge?.onTaskCompleted(
            BrowserDownloadResult(
                taskId = task.id,
                title = task.title,
                filePath = task.filePath,
                success = task.status == com.avd.browserkit.download.BrowserDownloadStatus.COMPLETED,
            ),
        )
    }

    fun notifyBridgeFailed(taskId: String, message: String) {
        BrowserKitLog.e("Bridge.Notify", "failed taskId=$taskId message=$message")
        downloadBridge?.onTaskFailed(taskId, message)
    }
}
