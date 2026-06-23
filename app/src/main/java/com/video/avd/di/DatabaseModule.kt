package com.video.avd.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.video.avd.constent.DatabaseConstant
import com.video.avd.data.local.AppDatabase
import com.video.avd.data.local.videosDataBase.FoldersDao
import com.video.avd.data.local.videosDataBase.VideosDao
import com.video.avd.repo.AllVideoRepository
import com.video.avd.repo.FolderRepository
import com.video.avd.repo.Repository
import com.video.avd.repo.VideoRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    fun provideFolderDao(appDatabase: AppDatabase) : FoldersDao {
        return appDatabase.folderdao()
    }


    @Provides
    fun videoDao(appDatabase: AppDatabase):VideosDao{
        return appDatabase.videosDao()
    }


    @Provides
    @Singleton
    fun getContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }


    @Singleton
    @Provides
    fun getContext(@ApplicationContext context: Context): Context {
        return context
    }


    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            DatabaseConstant.DB_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Singleton
    @Provides
    fun providesRepository(appDatabase: AppDatabase) = Repository(appDatabase)


    @Singleton
    @Provides
    fun providesallvideoRepository(appDatabase: AppDatabase,repository: Repository) = AllVideoRepository(appDatabase,repository)



    @Singleton
    @Provides
    fun providesVideoRepository(appDatabase: AppDatabase,repository: Repository) = VideoRepository(appDatabase,repository)

    @Singleton
    @Provides
    fun providesFolderRepository(appDatabase: AppDatabase) = FolderRepository(appDatabase)


}


