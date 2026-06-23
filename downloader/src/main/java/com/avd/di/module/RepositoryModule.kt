package com.avd.di.module

import com.avd.data.local.AdBlockHostsLocalDataSource
import com.avd.data.local.HistoryLocalDataSource
import com.avd.data.local.ProgressLocalDataSource
import com.avd.data.local.TopPagesLocalDataSource
import com.avd.data.local.VideoLocalDataSource
import com.avd.data.remote.AdBlockHostsRemoteDataSource
import com.avd.data.remote.TopPagesRemoteDataSource
import com.avd.data.remote.VideoRemoteDataSource
import com.avd.data.repository.AdBlockHostsRepository
import com.avd.data.repository.AdBlockHostsRepositoryImpl
import com.avd.data.repository.HistoryRepository
import com.avd.data.repository.HistoryRepositoryImpl
import com.avd.data.repository.ProgressRepository
import com.avd.data.repository.ProgressRepositoryImpl
import com.avd.data.repository.TopPagesRepository
import com.avd.data.repository.TopPagesRepositoryImpl
import com.avd.data.repository.VideoRepository
import com.avd.data.repository.VideoRepositoryImpl
import com.avd.di.qualifier.LocalData
import com.avd.di.qualifier.RemoteData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Make this module available in the SingletonComponent
abstract class RepositoryModule {

//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindConfigLocalDataSource(localDataSource: ConfigLocalDataSource): ConfigRepository

//    @Singleton
//    @Binds
//    @RemoteData
//    abstract fun bindConfigRemoteDataSource(remoteDataSource: ConfigRemoteDataSource): ConfigRepository
//
//    @Singleton
//    @Binds
//    abstract fun bindConfigRepositoryImpl(configRepository: ConfigRepositoryImpl): ConfigRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindTopPagesLocalDataSource(localDataSource: TopPagesLocalDataSource): TopPagesRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindTopPagesRemoteDataSource(remoteDataSource: TopPagesRemoteDataSource): TopPagesRepository

    @Singleton
    @Binds
    abstract fun bindTopPagesRepositoryImpl(topPagesRepository: TopPagesRepositoryImpl): TopPagesRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindVideoLocalDataSource(localDataSource: VideoLocalDataSource): VideoRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindVideoRemoteDataSource(remoteDataSource: VideoRemoteDataSource): VideoRepository

    @Singleton
    @Binds
    abstract fun bindVideoRepositoryImpl(videoRepository: VideoRepositoryImpl): VideoRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindProgressLocalDataSource(localDataSource: ProgressLocalDataSource): ProgressRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindHistoryLocalDataSource(localDataSource: HistoryLocalDataSource): HistoryRepository

    @Singleton
    @Binds
    abstract fun bindProgressRepositoryImpl(progressRepository: ProgressRepositoryImpl): ProgressRepository

    @Singleton
    @Binds
    abstract fun bindHistoryRepositoryImpl(historyRepository: HistoryRepositoryImpl): HistoryRepository

    @Singleton
    @Binds
    @LocalData
    abstract fun bindAdBlockHostsLocalDataSource(adBlockHostsLocalDataSource: AdBlockHostsLocalDataSource): AdBlockHostsRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindAdBlockHostsRemoteDataSource(adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource): AdBlockHostsRepository

    @Singleton
    @Binds
    abstract fun bindAdBlockHostsRepositoryImpl(adBlockHostsRepository: AdBlockHostsRepositoryImpl): AdBlockHostsRepository
}
