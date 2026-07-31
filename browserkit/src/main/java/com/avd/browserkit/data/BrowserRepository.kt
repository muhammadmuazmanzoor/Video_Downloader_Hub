package com.avd.browserkit.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserRepository(context: Context) {
    private val db = BrowserKitDatabase.get(context)
    private val gson = Gson()

    suspend fun getActiveDownloads() = withContext(Dispatchers.IO) {
        db.downloadDao().getActive()
    }

    suspend fun getFinishedDownloads() = withContext(Dispatchers.IO) {
        db.downloadDao().getFinished()
    }

    suspend fun getAllDownloads() = withContext(Dispatchers.IO) {
        db.downloadDao().getAll()
    }

    suspend fun upsertDownload(entity: BrowserDownloadEntity) = withContext(Dispatchers.IO) {
        db.downloadDao().upsert(entity)
    }

    suspend fun getDownload(id: String) = withContext(Dispatchers.IO) {
        db.downloadDao().getById(id)
    }

    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        db.downloadDao().delete(id)
    }

    suspend fun addHistory(title: String, url: String) = withContext(Dispatchers.IO) {
        db.historyDao().insert(
            BrowserHistoryEntity(
                title = title.ifBlank { url },
                url = url,
                visitedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getHistory() = withContext(Dispatchers.IO) {
        db.historyDao().getAll()
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        db.historyDao().deleteById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        db.historyDao().clear()
    }

    suspend fun addBookmark(title: String, url: String) = withContext(Dispatchers.IO) {
        db.bookmarkDao().insert(
            BrowserBookmarkEntity(
                title = title.ifBlank { url },
                url = url,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getBookmarks() = withContext(Dispatchers.IO) {
        db.bookmarkDao().getAll()
    }

    suspend fun removeBookmark(url: String) = withContext(Dispatchers.IO) {
        db.bookmarkDao().deleteByUrl(url)
    }

    fun headersToJson(headers: Map<String, String>): String? {
        if (headers.isEmpty()) return null
        return gson.toJson(headers)
    }

    fun headersFromJson(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return runCatching { gson.fromJson<Map<String, String>>(json, type) }.getOrDefault(emptyMap())
    }
}
