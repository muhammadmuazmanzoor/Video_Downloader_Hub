package com.video.avd.utils

import android.content.Context

class SharedPreferencesManager(context: Context) {
    private val preferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONVERSION_COUNT = "ConversionCount"
        private const val KEY_FIRST_TIME = "key_first_time"
        private const val LAST_DETECTED = "last_detected"
    }

    fun saveConversionCount(count: Int) {
        val editor = preferences.edit()
        editor.putInt(KEY_CONVERSION_COUNT, count)
        editor.apply()
    }
    fun saveSubtitlePosition(context: Context, key: String, value: String) {
        // Use SharedPreferences.Editor to save the value
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.apply() // Use apply() for asynchronous saving
    }
    fun getSubtitlePosition(context: Context, key: String, defaultValue: String): String {
        // Retrieve the value using the key
        return preferences.getString(key, defaultValue) ?: defaultValue
    }
    fun getConversionCount(): Int {
        return preferences.getInt(KEY_CONVERSION_COUNT, 0)
    }

    fun isFirstTime(): Boolean {
        return preferences.getBoolean(KEY_FIRST_TIME, true)
    }

    fun setFirstTime(isFirstTime: Boolean) {
        preferences.edit().putBoolean(KEY_FIRST_TIME, isFirstTime).apply()
    }

    fun savePlayCountHDR(count: Int) {
        preferences?.edit()?.putInt("hdr_count",count)?.apply()
    }

    fun getPlayCountHDR(): Int {
        return preferences?.getInt("hdr_count",0) ?: 0
    }

    fun setLastLink(lastdetected:String){
        preferences.edit().putString(LAST_DETECTED, lastdetected).apply()
    }

    fun getLastDetected(): String {
        return preferences?.getString(LAST_DETECTED,"") ?: ""
    }

}