package com.avd.di.module

import com.avd.util.scheduler.BaseSchedulers
import com.avd.util.scheduler.BaseSchedulersImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class) // Install this module in Hilt's SingletonComponent
abstract class AppModule {

    @Binds
    abstract fun bindBaseSchedulers(baseSchedulers: BaseSchedulersImpl): BaseSchedulers


}
