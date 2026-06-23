package com.video.avd.di

import com.video.avd.utils.AppUtils
import com.video.avd.utils.WeakReferenceVideo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DependencyModule {

    @Provides
    @Singleton
    fun providefolderweakrefrence() : WeakReferenceVideo {
        return WeakReferenceVideo()
    }


    @Singleton
    @Provides
    fun provideAppUtil(): AppUtils {
        return AppUtils
    }

}