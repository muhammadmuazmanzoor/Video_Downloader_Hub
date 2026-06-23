package com.avd.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class DataStoreManager@Inject constructor(private val context: Context) {

    companion object {
        val NOTIFICATION_DENIAL_COUNT = intPreferencesKey("notification_denial_count")
    }

    suspend fun incrementDenialCount() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[NOTIFICATION_DENIAL_COUNT] ?: 0
            preferences[NOTIFICATION_DENIAL_COUNT] = currentCount + 1
        }
    }

    suspend fun resetDenialCount() {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_DENIAL_COUNT] = 0
        }
    }
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("DATA_STORE_AVD",
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ))

    private suspend fun <T> DataStore<Preferences>.getFromLocalStorage(PreferencesKey: Preferences.Key<T>, value: T, func: T.() -> Unit) {
        data.catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }.map {
            it[PreferencesKey]?: value
        }.collect {
            it?.let { func.invoke(it as T) }
        }
    }

    private suspend fun <T> storeValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    private suspend fun <T> readValue(key: Preferences.Key<T>, value: T, responseFunc: T.() -> Unit) {
        context.dataStore.getFromLocalStorage(key, value) {
            responseFunc.invoke(this)
        }
    }

    fun <T> readDataStoreValue(key: Preferences.Key<T>, defaultValue: T, onCompleted: T.() -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            readValue(key, defaultValue) {
                if (this == null) {
                    writeDataStoreValue(key, defaultValue)
                } else {
                    onCompleted.invoke(this)
                }
            }
        }
    }

    fun <T> writeDataStoreValue(key: Preferences.Key<T>, value: T) {
        CoroutineScope(Dispatchers.Main).launch {
            storeValue(key, value)
            Log.e("Languageset",value.toString())
        }
    }
}