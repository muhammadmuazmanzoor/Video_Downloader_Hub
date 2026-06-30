package com.avd.util

import android.util.Log
import com.avd.R
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

object RemoteConfigHelper {
    private val remoteConfig = Firebase.remoteConfig

    fun init() {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            Log.d("RemoteConfig", "fetch success: ${task.isSuccessful}")
        }
    }

    fun getSocialDownloaderBaseUrl(): String {
        Log.d("RemoteConfig", "getSocialDownloaderBaseUrl: ${remoteConfig.getString("social_downloader_base_url")}")
        return remoteConfig.getString("social_downloader_base_url")
            .ifEmpty { "https://ai-livewallpaper-backend.aspire.pics/" }
    }

    fun getSocialDownloaderEndpoint(): String {
        Log.d("RemoteConfig", "getSocialDownloaderEndpoint: ${remoteConfig.getString("social_downloader_endpoint")}")
        return remoteConfig.getString("social_downloader_endpoint")
            .ifEmpty { "api/v1/video/download" }
    }
}