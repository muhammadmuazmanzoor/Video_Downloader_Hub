package com.avd.util

import android.util.Log
import com.avd.R
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

object RemoteConfigHelper {
    private val remoteConfig = Firebase.remoteConfig
    private const val SOCIAL_DOWNLOADER_BASE_URL = "https://backend-video-downloader.aspire.pics/"
    private const val SOCIAL_DOWNLOADER_ENDPOINT = "api/v1/video/download"
    private const val SOCIAL_DOWNLOADER_API_KEY = "396f7824p121f56jsnfdcae46a09baf2cmsh7ba6df8f2dee4c0peeaf742abe7e0fp19a49ajsn9e4"
    private const val SOCIAL_DOWNLOADER_APP_LABEL = "hub"

    fun init() {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            Log.d("RemoteConfig", "fetch success: ${task.isSuccessful}")
        }
    }

    fun getSocialDownloaderBaseUrl(): String {
        Log.d("RemoteConfig", "getSocialDownloaderBaseUrl: ${remoteConfig.getString("social_downloader_base_url")}")
        return SOCIAL_DOWNLOADER_BASE_URL
        // To enable remote base URL again later:
        // return remoteConfig.getString("social_downloader_base_url")
        //     .ifEmpty { SOCIAL_DOWNLOADER_BASE_URL }
    }

    fun getSocialDownloaderEndpoint(): String {
        Log.d("RemoteConfig", "getSocialDownloaderEndpoint: ${remoteConfig.getString("social_downloader_endpoint")}")
        return SOCIAL_DOWNLOADER_ENDPOINT
        // To enable remote endpoint again later:
        // return remoteConfig.getString("social_downloader_endpoint")
        //     .ifEmpty { SOCIAL_DOWNLOADER_ENDPOINT }
    }

    fun getSocialDownloaderApiKey(): String = SOCIAL_DOWNLOADER_API_KEY

    fun getSocialDownloaderAppLabel(): String = SOCIAL_DOWNLOADER_APP_LABEL

    fun getSocialDownloaderHeaders(): Map<String, String> {
        return mapOf(
            "api-key" to SOCIAL_DOWNLOADER_API_KEY,
            "x-api-key" to SOCIAL_DOWNLOADER_API_KEY,
            "X-App-Label" to SOCIAL_DOWNLOADER_APP_LABEL
        )
    }
}
