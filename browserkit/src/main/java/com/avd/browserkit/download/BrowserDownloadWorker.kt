package com.avd.browserkit.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

abstract class BrowserDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_TASK_ID = "bk_task_id"
        const val KEY_TITLE = "bk_title"
        const val KEY_PAGE_URL = "bk_page_url"
        const val KEY_DOWNLOAD_URL = "bk_download_url"
        const val KEY_QUALITY = "bk_quality"
        const val KEY_STREAM_TYPE = "bk_stream_type"
        const val KEY_HEADERS_JSON = "bk_headers_json"
    }

    protected val taskId: String
        get() = inputData.getString(KEY_TASK_ID).orEmpty()

    protected val title: String
        get() = inputData.getString(KEY_TITLE).orEmpty()

    protected val downloadUrl: String
        get() = inputData.getString(KEY_DOWNLOAD_URL).orEmpty()

    protected fun headersMap(): Map<String, String> {
        val json = inputData.getString(KEY_HEADERS_JSON)
        return BrowserRepositoryHeaders.fromJson(applicationContext, json)
    }
}

internal object BrowserRepositoryHeaders {
    fun fromJson(context: Context, json: String?): Map<String, String> {
        return com.avd.browserkit.data.BrowserRepository(context).headersFromJson(json)
    }
}
