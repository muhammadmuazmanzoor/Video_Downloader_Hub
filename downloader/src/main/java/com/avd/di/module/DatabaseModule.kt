package com.avd.di.module

import android.content.Context
import androidx.room.Room
import com.avd.data.local.room.AppDatabaseDownloader
import com.avd.data.local.room.dao.AdHostDao
import com.avd.data.local.room.dao.ConfigDao
import com.avd.data.local.room.dao.HistoryDao
import com.avd.data.local.room.dao.PageDao
import com.avd.data.local.room.dao.ProgressDao
import com.avd.data.local.room.dao.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabaseDownloader {
        return Room.databaseBuilder(
            context,
            AppDatabaseDownloader::class.java,
            "dl.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideConfigDao(database: AppDatabaseDownloader): ConfigDao = database.configDao()


    @Provides
    fun provideCommentDao(database: AppDatabaseDownloader): VideoDao = database.videoDao()


    @Provides
    fun provideProgressDao(database: AppDatabaseDownloader): ProgressDao = database.progressDao()


    @Provides
    fun provideHistoryDao(database: AppDatabaseDownloader): HistoryDao = database.historyDao()


    @Provides
    fun providePageDao(database: AppDatabaseDownloader): PageDao = database.pageDao()


    @Provides
    fun provideAdHostDao(database: AppDatabaseDownloader): AdHostDao = database.adHostDao()
}
