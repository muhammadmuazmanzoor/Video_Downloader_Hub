package com.video.avd.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.video.avd.ui.homeVideo.model.FeatureModel
import com.video.avd.ui.player.PlayerVideoActivity
import java.lang.ref.WeakReference
import java.util.Locale

object AppPreference {

    var isFromSplash: Boolean = false
    private const val PREFS_NAME = "MyPrefs"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val PREF_PERMISSIONS_CHECK = "is_permission_granted_for_status"
    private const val KEY_PERMISSIONS_CHECK = "is_permission_granted"
    private const val KEY_PERMISSIONS_CHECK_BUSINESS = "is_permission_granted_business"
    private const val KEY_WHATSAPP_SELECTED = "business"
    private const val KEY_HAVE_SUBTITLE = "subtitle-added"
    private const val KEY_SUBTITLE_ENABLED = "subtitle-enabled"
    private const val KEY_VIEW_TYPE = "grid_view"
    private const val KEY_SORT_TYPE = "sort_type"
    private const val KEY_AUDIOS_SORT_TYPE = "audio_sort_type"
    private const val KEY_POPUP_ORDER_TYPE = "order_type"
    private const val KEY_VIDEO_PLAYER_PLAYLIST_ORDER_TYPE = "order_type"
    private const val CHROME_LIST_POSITION = "mPosition"
    private const val VISITED_PLAYER_FIRST_TIME = "visited_player"
    private const val IS_IN_APP_REVIEW_SHOWN = "review_shown"
    private const val KEY_SHOW_DOWNLOAD_GUIDANCE = "show"
    private const val KEY_SHOW_WHATSAPP_GUIDANCE = "SH"
    private const val KEY_SHOW_PRO_PANEL = "pro"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_DEFAULT_LANGUAGE = "system_language"
    private const val KEY_24HOUR_TIME = "Timeof24hours"
    private const val KEY_IS24_HOUR_ENABLED = "24hourenabled"
    private const val KEY_30_MINUTES_DIALOGE_SHOWN_TIME = "dialoge_shown_time"
    private const val KEY_IS30_Minutes_ENABLED = "30minutesenabled"
    private const val KEY_REWARDED_WATCHED_COUNT = "RewardedCount"
    private const val IS_HISTORY_ON = "history"
    private const val KEY_USING_SW_DECODER = "is_using_sw_decoder"
    private const val KEY_FEATURE_LIST = "feature_list_new"


    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveFeatureList(context: Context, list: List<FeatureModel?>) {
        val gson = Gson()
        val json = gson.toJson(list)
        getPrefs(context).edit().putString(KEY_FEATURE_LIST, json).apply()
    }

    fun loadFeatureList(context: Context): List<FeatureModel?> {
        val gson = Gson()
        val json = getPrefs(context).getString(KEY_FEATURE_LIST, null)
        val type = object : TypeToken<List<FeatureModel?>>() {}.type
        return gson.fromJson(json, type) ?: listOf()
    }
    // Method to save the SW decoder preference
    fun saveSWDecoderPreference(context: Context, isUsingSWDecoder: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USING_SW_DECODER, isUsingSWDecoder).apply()
    }

    // Method to retrieve the SW decoder preference
    fun isUsingSWDecoder(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default value is true (using SW decoder by default)
        return prefs.getBoolean(KEY_USING_SW_DECODER, true)
    }

    fun isPermissionGrantedForStatus(context: Context): Boolean {
        val isPermissionGrantedForStatus: SharedPreferences = context.getSharedPreferences(
            PREF_PERMISSIONS_CHECK, Context.MODE_PRIVATE
        )
        return isPermissionGrantedForStatus.getBoolean(KEY_PERMISSIONS_CHECK, false)
    }



    fun setPermissionForStatus(context: Context, value: Boolean) {
        val isPermissionGrantedForStatus: SharedPreferences = context.getSharedPreferences(
            PREF_PERMISSIONS_CHECK, Context.MODE_PRIVATE
        )
        isPermissionGrantedForStatus.edit().putBoolean(KEY_PERMISSIONS_CHECK, value).apply()
    }

    fun setPermissionForStatusBusiness(context: Context, value: Boolean) {
        val isPermissionGrantedForStatus: SharedPreferences = context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )
        isPermissionGrantedForStatus.edit().putBoolean(KEY_PERMISSIONS_CHECK_BUSINESS, value).apply()
    }
    fun isPermissionGrantedForStatusBusiness(context: Context): Boolean {
        val isPermissionGrantedForStatus: SharedPreferences = context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )
        return isPermissionGrantedForStatus.getBoolean(KEY_PERMISSIONS_CHECK_BUSINESS, false)
    }

    fun setWhatsappSelected(context: Context, value: Boolean) {
        val isBusiness: SharedPreferences = context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )
        isBusiness.edit().putBoolean(KEY_WHATSAPP_SELECTED, value).apply()
    }
    fun isWhatsappBusinessSelected(context: Context): Boolean {
        val isBusiness: SharedPreferences = context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )
        return isBusiness.getBoolean(KEY_WHATSAPP_SELECTED, false)
    }

    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }



    fun isHistoryOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(IS_HISTORY_ON, true)
    }

    fun hasVisitedPlayerFirstTime(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(VISITED_PLAYER_FIRST_TIME, false)
    }

    fun setHasVisitedPlayerFirstTime(context: Context, isFirstTime: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(VISITED_PLAYER_FIRST_TIME, isFirstTime).apply()
    }

    fun isInAppDialogeShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(IS_IN_APP_REVIEW_SHOWN, false)
    }

    fun setIsInAppDialShown(context: Context, isToastShown: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(IS_IN_APP_REVIEW_SHOWN, isToastShown).apply()
    }

    fun setFirstLaunch(context: Context, isFirstLaunch: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch).apply()
    }


    fun setHistoryOn(context: Context, isFirstLaunch: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(IS_HISTORY_ON, isFirstLaunch).apply()
    }

    fun isFirstPlayLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getBoolean("New", false)
        return value
    }

    fun setFirstPlayLaunch(context: Context, isFirstLaunch: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("New", isFirstLaunch).apply()
        PlayerVideoActivity.isFirstPlayLaunch = true
    }

    fun saveToPrefs(context: Context?, key: String?, value: Boolean) {
        val contextWeakReference: WeakReference<Context> = WeakReference<Context>(context)
        if (contextWeakReference.get() != null) {
            val prefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(contextWeakReference.get())
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(key, value)
            editor.commit()
        }
    }

    fun setPlaystorePrefs(context: Context) {
        try {
            val sharedPref = context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("key3", true)
            editor.apply()
        } catch (e: java.lang.Exception) {
            //
        }

    }

    fun getPlaystorePrefs(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
        val value = sharedPref.getBoolean("key3", false)
        return value
    }

    fun saveChromeListPosition(context: Context, position: Int) {
        val sharedPref = context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(CHROME_LIST_POSITION, position)
    }

    fun getChromeListPosition(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(CHROME_LIST_POSITION, 2)
    }

    fun getFromPrefs(context: Context?, key: String?, defaultValue: Boolean): Boolean {
        val contextWeakReference = WeakReference(context)
        if (contextWeakReference.get() != null) {
            val sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(contextWeakReference.get())
            return try {
                sharedPrefs.getBoolean(key, defaultValue)
            } catch (e: Exception) {
                Log.e("Execption", e.localizedMessage)
                defaultValue
            }
        }
        return defaultValue
    }

    fun removeFromPrefs(context: Context?, key: String?) {
        val contextWeakReference = WeakReference(context)
        if (contextWeakReference.get() != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(contextWeakReference.get())
            val editor = prefs.edit()
            editor.remove(key)
            editor.commit()
        }
    }

    fun getSubtitleUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HAVE_SUBTITLE, "")
    }

    fun saveSubtitleUri(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HAVE_SUBTITLE, uri).apply()
    }

    fun enableSubtitle(context: Context, isEnable: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SUBTITLE_ENABLED, isEnable).apply()
    }

    fun isSubtitleEnabled(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SUBTITLE_ENABLED, false)
    }

    fun saveViewType(context: Context, viewType: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_VIEW_TYPE, viewType).apply()
    }

    fun getViewType(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_VIEW_TYPE, 0)
    }

    fun saveSortType(context: Context, viewType: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SORT_TYPE, viewType).apply()
    }

    fun getSortType(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SORT_TYPE, 2)
    }

    fun saveAudiosSortType(context: Context, viewType: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_AUDIOS_SORT_TYPE, viewType).apply()
    }

    fun getAudiosSortType(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_AUDIOS_SORT_TYPE, 2)
    }

    fun savePopupOrderType(context: Context, viewType: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_POPUP_ORDER_TYPE, viewType).apply()

    }

    fun saveVideoPlaylistOrderType(context: Context, viewType: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_VIDEO_PLAYER_PLAYLIST_ORDER_TYPE, viewType).apply()
    }

    fun getPopupOrderType(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_POPUP_ORDER_TYPE, 0)

    }

    fun getVideoPlayerPopupOrderType(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_VIDEO_PLAYER_PLAYLIST_ORDER_TYPE, 0)
    }

    fun saveDownloadGuidanceState(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_DOWNLOAD_GUIDANCE, show).apply()
    }

    fun getDownloadGuidanceState(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_DOWNLOAD_GUIDANCE, false)
    }

    fun saveWhatsappGuidanceState(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_WHATSAPP_GUIDANCE, show).apply()
    }

    fun getWhatsappGuidanceState(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_WHATSAPP_GUIDANCE, false)
    }

    fun saveProPanelVisibility(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_PRO_PANEL, show).apply()
    }

    fun showProPanelVisibility(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_PRO_PANEL, false)
    }

    fun saveLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getLanguage(context: Context): String? {
        val systemLang = Locale.getDefault().language

        // List of supported languages in your app
        val supportedLangs = listOf("ar","ko", "ja", "es","in","pt","fr", "vi","ru","tr", "ms","th","pl")

        // Check if the system language is in the list of supported languages, else default to English
        var lange = if (systemLang in supportedLangs) systemLang else "en"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_LANGUAGE, lange)
        return lang
    }

    fun saveSystemDefaultLanguage(context: Context) {
        val systemLang = Locale.getDefault().language
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_LANGUAGE, systemLang).apply()
    }
    fun getSystemDefaultLanguage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_DEFAULT_LANGUAGE, "en")
        return lang
    }

    fun save24HoursEnabledTime(context: Context, timeStamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_24HOUR_TIME, timeStamp).apply()
    }

    fun get24HoursEnabledTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        return prefs.getLong(KEY_24HOUR_TIME, currentTime)
    }

    fun save24HoursEnabledValue(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS24_HOUR_ENABLED, isEnabled).apply()
    }

    fun get24HoursEnabledValue(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS24_HOUR_ENABLED, false)
    }

    fun save30MinutesEnabledValue(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS30_Minutes_ENABLED, isEnabled).apply()
    }

    fun get30MinutesEnabledValue(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS30_Minutes_ENABLED, false)
    }


    fun save24HourRewardedWatchedCount(context: Context, count: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REWARDED_WATCHED_COUNT, count).apply()
    }

    fun get24HourRewardedWatchedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REWARDED_WATCHED_COUNT, 0)
    }

    fun save30MinutesShownTime(context: Context, timeStamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_30_MINUTES_DIALOGE_SHOWN_TIME, timeStamp).apply()
    }

    fun get30MinutesShownTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        return prefs.getLong(KEY_30_MINUTES_DIALOGE_SHOWN_TIME, currentTime)
    }

}