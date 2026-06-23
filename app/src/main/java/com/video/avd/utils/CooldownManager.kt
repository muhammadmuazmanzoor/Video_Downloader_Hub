package com.video.avd.utils

import android.content.Context

class CooldownManager(private val context: Context) {

    private val sharedPref = context.getSharedPreferences("cooldown_prefs", Context.MODE_PRIVATE)
    private val LAST_TIME_KEY = "last_saved_time"

    // Call this whenever you want to save current time
    fun saveCurrentTime() {
        val currentTime = System.currentTimeMillis()
        sharedPref.edit().putLong(LAST_TIME_KEY, currentTime).apply()
    }

    // This is your main function: returns true if enough time has passed
    fun isCooldownOver(remoteConfigValueInSeconds: Long): Boolean {
        val lastSavedTime = sharedPref.getLong(LAST_TIME_KEY, 0L)
        val currentTime = System.currentTimeMillis()

        val cooldownMillis = remoteConfigValueInSeconds * 1000

        return currentTime >= (lastSavedTime + cooldownMillis)
    }

    // Optional: you can reset if you need
    fun clearSavedTime() {
        sharedPref.edit().remove(LAST_TIME_KEY).apply()
    }
}
