package com.avd.browserkit.download

enum class BrowserDownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
}

data class BrowserDownloadTask(
    val id: String,
    val title: String,
    val pageUrl: String,
    val downloadUrl: String,
    val qualityLabel: String,
    val streamType: String,
    val headers: Map<String, String> = emptyMap(),
    val percent: Int = 0,
    val status: BrowserDownloadStatus = BrowserDownloadStatus.QUEUED,
    val filePath: String? = null,
    val workerId: String? = null,
)
