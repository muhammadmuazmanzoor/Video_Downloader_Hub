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

      //  return SOCIAL_DOWNLOADER_BASE_URL
        // To enable remote base URL again later:
         return remoteConfig.getString("social_downloader_base_url")
            .ifEmpty { SOCIAL_DOWNLOADER_BASE_URL }
        Log.d("RemoteConfig", "getSocialDownloaderBaseUrl: ${remoteConfig.getString("social_downloader_base_url")}")
    }

    fun getSocialDownloaderEndpoint(): String {
        // return SOCIAL_DOWNLOADER_ENDPOINT
        // To enable remote endpoint again later:
         return remoteConfig.getString("social_downloader_endpoint")
            .ifEmpty { SOCIAL_DOWNLOADER_ENDPOINT }
        Log.d("RemoteConfig", "getSocialDownloaderEndpoint: ${remoteConfig.getString("social_downloader_endpoint")}")
    }

    fun getSocialDownloaderApiKey(): String {
        Log.d("RemoteConfig", "getSocialDownloaderApiKey: ${remoteConfig.getString("social_downloader_api_key")}")
        return remoteConfig.getString("social_downloader_api_key")
            .ifEmpty { SOCIAL_DOWNLOADER_API_KEY }
    }

    fun getSocialDownloaderAppLabel(): String {
        return remoteConfig.getString("social_downloader_app_label")
            .ifEmpty {
                ContextUtils.getApplicationContext()?.packageName
                    ?.takeIf { it.isNotBlank() }
                    ?: SOCIAL_DOWNLOADER_APP_LABEL
            }
        Log.d("RemoteConfig", "getSocialDownloaderAppLabel: ${remoteConfig.getString("social_downloader_app_label")}")
    }

    fun getSocialDownloaderHeaders(): Map<String, String> {
        return mapOf(
            "api-key" to getSocialDownloaderApiKey(),
            "x-api-key" to getSocialDownloaderApiKey(),
            "X-App-Label" to getSocialDownloaderAppLabel()
        )
    }
}
