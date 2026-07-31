package com.avd.browserkit.ytdlp

import android.content.Context

/**
 * Contract implemented inside the on-demand [youtubedldynamic] feature module.
 * Base app talks to it via [YoutubeDlBridge] reflection so natives stay out of install-time AAB.
 */
interface YoutubeDlEngine {
    fun init(context: Context)

    fun getInfo(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): YtDlpRawInfo?

    fun execute(
        url: String,
        outputPath: String,
        headers: Map<String, String> = emptyMap(),
        facebookMode: Boolean = false,
        onProgress: (Float) -> Unit,
    )
}

data class YtDlpRawInfo(
    val title: String?,
    val url: String?,
    val ext: String?,
    val duration: Int,
    val manifestUrl: String?,
    val formats: List<YtDlpRawFormat>,
)

data class YtDlpRawFormat(
    val url: String,
    val height: Int,
    val formatNote: String?,
    val ext: String?,
    val formatId: String?,
    val manifestUrl: String?,
)
