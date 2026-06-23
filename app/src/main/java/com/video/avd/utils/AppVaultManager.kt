package com.video.avd.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.video.avd.ui.player.subtitle.SubtitleCustomizationValues
import javax.inject.Inject

class AppVaultManager @Inject constructor(context: Context) {

    companion object {
        private const val PREF_NAME = "app_vault_pref"
        private const val KEY_PIN = "pin"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"

        private const val KEY_AUDIO_PLAY_PAUSE = "play-pause"
        private const val KEY_SUBTITLE_VALUES = "subtitle-values"
    }


    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun savePin(pin: String) {
        sharedPreferences.edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(): String? {
        return sharedPreferences.getString(KEY_PIN, null)
    }

    fun clearPin() {
        sharedPreferences.edit().remove(KEY_PIN).apply()
    }

    fun hasPin(): Boolean {
        return sharedPreferences.contains(KEY_PIN)
    }

    fun saveSecurityQuestion(question: String) {
        sharedPreferences.edit().putString(KEY_SECURITY_QUESTION, question).apply()
    }

    fun getSecurityQuestion(): String? {
        return sharedPreferences.getString(KEY_SECURITY_QUESTION, null)
    }

    fun saveSecurityAnswer(answer: String) {
        sharedPreferences.edit().putString(KEY_SECURITY_ANSWER, answer).apply()
    }

    fun getSecurityAnswer(): String? {
        return sharedPreferences.getString(KEY_SECURITY_ANSWER, null)
    }

    fun clearSecurityData() {
        sharedPreferences.edit()
            .remove(KEY_SECURITY_QUESTION)
            .remove(KEY_SECURITY_ANSWER)
            .apply()
    }

    fun hasSecurityData(): Boolean {
        return sharedPreferences.contains(KEY_SECURITY_QUESTION) && sharedPreferences.contains(KEY_SECURITY_ANSWER)
    }

    /**
     *These below two methods are not part of App Vault. it is about to save,
     * if audio song is Played or Paused by user to maintain its state
     * after returning to AudioPlayerFragment from Notification
     */
    fun savePlayPauseState(isPlay:Boolean) = sharedPreferences.edit().putBoolean(KEY_AUDIO_PLAY_PAUSE, isPlay).apply()

    fun getPlayPauseState():Boolean = sharedPreferences.getBoolean(KEY_AUDIO_PLAY_PAUSE, true)


    /**
     methods for save and get subtitle customization values
     */
    fun saveSubtitleValues(values: SubtitleCustomizationValues){
        val json= Gson().toJson(values)
        sharedPreferences.edit().putString(KEY_SUBTITLE_VALUES,json).apply()
    }

    fun getSubtitleValues(): SubtitleCustomizationValues? {
        val values: SubtitleCustomizationValues
        val gson = Gson()
        val valueJson = sharedPreferences.getString(KEY_SUBTITLE_VALUES, null)
        return try {
            values = gson.fromJson(valueJson, SubtitleCustomizationValues::class.java)
            values
        } catch (e: Exception) {
            null
        }
    }
}
