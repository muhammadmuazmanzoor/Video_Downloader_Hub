package com.video.avd.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
//import com.myAllVideoBrowser.util.Memory
//import com.xilliapps.hdvideoplayer.ui.video_downloader_new.service.CustomProxyController
//import com.xilliapps.hdvideoplayer.ui.video_downloader_new.service.OkHttpProxyClient
//import com.xilliapps.hdvideoplayer.ui.video_downloader_new.service.VideoService
import com.video.avd.utils.InAppPurchases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun getInAppPurchase(
        context: Context
    ): InAppPurchases {
        return InAppPurchases(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(application: Application): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Set higher connection timeout
            .readTimeout(300, TimeUnit.SECONDS)   // Set higher read timeout
            .writeTimeout(300, TimeUnit.SECONDS)  // Set higher write timeout

            .build()
    }

@Provides
@Singleton
fun provideGson(): Gson {
    return GsonBuilder().create()
}

}