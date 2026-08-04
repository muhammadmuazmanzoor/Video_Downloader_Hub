package com.avd.util

import android.content.Context
import android.provider.Settings
import java.util.UUID

object SocialDownloaderIdentity {
    private const val PREFS_NAME = "social_downloader_identity"
    private const val KEY_INSTALLATION_ID = "installation_id"

    fun getInstallationId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, created).apply()
        return created
    }

    fun getPackageName(context: Context): String = context.packageName

    fun getDeviceId(context: Context): String {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty().ifBlank { "unknown_device" }
    }
}
