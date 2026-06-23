package com.avd.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.avd.data.local.room.dao.*
import com.avd.data.local.room.entity.*

const val DB_VERSION = 1

@Database(
    entities = [PageInfo::class, SupportedPage::class, VideoInfo::class, ProgressInfo::class, HistoryItem::class, AdHost::class],
    version = DB_VERSION,
)

@TypeConverters(FormatsConverter::class, DownloadUrlsConverter::class)
abstract class AppDatabaseDownloader : RoomDatabase() {

    abstract fun configDao(): ConfigDao

    abstract fun videoDao(): VideoDao

    abstract fun progressDao(): ProgressDao

    abstract fun pageDao(): PageDao

    abstract fun historyDao(): HistoryDao

    abstract fun adHostDao(): AdHostDao
}