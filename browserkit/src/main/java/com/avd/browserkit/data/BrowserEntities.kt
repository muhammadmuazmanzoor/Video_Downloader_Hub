package com.avd.browserkit.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "browser_download_tasks")
data class BrowserDownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val pageUrl: String,
    val downloadUrl: String,
    val qualityLabel: String,
    val streamType: String,
    val headersJson: String?,
    val percent: Int,
    val status: String,
    val filePath: String?,
    val workerId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long,
)

@Entity(tableName = "browser_bookmarks")
data class BrowserBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long,
)

@Dao
interface BrowserDownloadDao {
    @Query("SELECT * FROM browser_download_tasks ORDER BY updatedAt DESC")
    suspend fun getAll(): List<BrowserDownloadEntity>

    @Query("SELECT * FROM browser_download_tasks WHERE status IN ('QUEUED','DOWNLOADING','PAUSED') ORDER BY updatedAt DESC")
    suspend fun getActive(): List<BrowserDownloadEntity>

    @Query("SELECT * FROM browser_download_tasks WHERE status IN ('COMPLETED','FAILED') ORDER BY updatedAt DESC")
    suspend fun getFinished(): List<BrowserDownloadEntity>

    @Query("SELECT * FROM browser_download_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BrowserDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: BrowserDownloadEntity)

    @Query("DELETE FROM browser_download_tasks WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BrowserHistoryDao {
    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT 200")
    suspend fun getAll(): List<BrowserHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM browser_history")
    suspend fun clear()
}

@Dao
interface BrowserBookmarkDao {
    @Query("SELECT * FROM browser_bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BrowserBookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BrowserBookmarkEntity)

    @Query("DELETE FROM browser_bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}
