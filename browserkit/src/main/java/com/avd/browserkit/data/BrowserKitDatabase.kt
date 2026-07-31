package com.avd.browserkit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BrowserDownloadEntity::class,
        BrowserHistoryEntity::class,
        BrowserBookmarkEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BrowserKitDatabase : RoomDatabase() {
    abstract fun downloadDao(): BrowserDownloadDao
    abstract fun historyDao(): BrowserHistoryDao
    abstract fun bookmarkDao(): BrowserBookmarkDao

    companion object {
        @Volatile
        private var instance: BrowserKitDatabase? = null

        fun get(context: Context): BrowserKitDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrowserKitDatabase::class.java,
                    "browserkit.db",
                ).build().also { instance = it }
            }
        }
    }
}
