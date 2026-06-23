package com.avd.di.module


import android.util.Log
import com.avd.data.remote.service.ApiService
import com.avd.data.remote.service.ApiService2
import com.avd.data.remote.service.SocialDownloaderService
import com.avd.data.remote.service.VideoService
import com.avd.data.remote.service.VideoServiceLocal
import com.avd.data.remote.service.YoutubedlHelper
import com.avd.di.qualifier.BaseUrl1
import com.avd.di.qualifier.BaseUrl2
import com.avd.di.qualifier.SocialDownloaderBaseUrl
import com.avd.util.RemoteConfigHelper
import com.avd.util.proxy_utils.CustomProxyController
import com.avd.util.proxy_utils.OkHttpProxyClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideVideoService(
        proxyController: CustomProxyController,
        okHttpProxyClient: OkHttpProxyClient
    ): VideoService = VideoServiceLocal(proxyController, provideYoutubeHelper(okHttpProxyClient))


    @Provides
    @Singleton
    fun provideYoutubeHelper(okHttpProxyClient: OkHttpProxyClient): YoutubedlHelper = YoutubedlHelper(okHttpProxyClient)



    @Provides
    @Singleton
    @Named("RapidApiClient1")
    fun provideRapidApiOkHttpClient(@Named("RapidApiKey1") rapidApiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-key", rapidApiKey)
                    .addHeader("x-rapidapi-host", "all-media-downloader5.p.rapidapi.com")
                    .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }


    @Provides
    @Singleton
    @Named("RapidApiClient2")
    fun provideRapidApiOkHttpClient2(@Named("RapidApiKey2") rapidApiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-rapidapi-key", rapidApiKey)
                    .addHeader("x-rapidapi-host", "tiktok-download-without-watermark.p.rapidapi.com")
                    .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @BaseUrl1
    @Provides
    @Singleton
    fun provideRetrofit(@Named("RapidApiClient1") okHttpClient: OkHttpClient ,@BaseUrl1 baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    @BaseUrl2
    @Provides
    @Singleton
    fun provideRetrofit2(@Named("RapidApiClient2") okHttpClient: OkHttpClient ,@BaseUrl2 baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    @Provides
    @Singleton
    fun provideApiService(@BaseUrl1 retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideApiService2(@BaseUrl2 retrofit: Retrofit): ApiService2 {
        return retrofit.create(ApiService2::class.java)
    }

    @Provides
    @Named("RapidApiKey1")
    fun provideRapidApiKey(): String = "9b6a9e5a6bmsh648945b7d7f9a57p15a2d0jsn3a29bd6330b8"


    @Provides
    @Named("RapidApiKey2")
    fun provideRapidApiKey2(): String = "9b6a9e5a6bmsh648945b7d7f9a57p15a2d0jsn3a29bd6330b8"

    @BaseUrl1
    @Provides
    fun provideBaseUrl1(): String = "https://all-media-downloader5.p.rapidapi.com/"

    @BaseUrl2
    @Provides
    fun provideBaseUrl2(): String = "https://tiktok-download-without-watermark.p.rapidapi.com/"

    @Provides
    @Named("SocialDownloaderKey")
    fun provideSocialDownloaderKey(): String = "479ddba08f1091cdaf3d0ac95bf75b292eeb7c747be2140ad7d7c5cf47465070"


   @Provides
    @SocialDownloaderBaseUrl
    fun provideSocialDownloaderBaseUrl(): String =
        "https://ai-livewallpaper-backend.aspire.pics/"

    @Provides
    @Singleton
    @Named("SocialDownloaderClient")
    fun provideSocialDownloaderOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("api-key", "479ddba08f1091cdaf3d0ac95bf75b292eeb7c747be2140ad7d7c5cf47465070")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(90, TimeUnit.SECONDS) // increase connect timeout
            .readTimeout(90, TimeUnit.SECONDS)    // increase read timeout
            .writeTimeout(90, TimeUnit.SECONDS)   // increase write timeout
            .build()
    }

    @SocialDownloaderBaseUrl
    @Provides
    @Singleton
    fun provideSocialDownloaderRetrofit(
        @Named("SocialDownloaderClient") okHttpClient: OkHttpClient,
        @SocialDownloaderBaseUrl baseUrl: String
    ): Retrofit {
        val baseUrl= RemoteConfigHelper.getSocialDownloaderBaseUrl()
        Log.d("checkBaseUrl","$baseUrl")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSocialDownloaderService(
        @SocialDownloaderBaseUrl retrofit: Retrofit
    ): SocialDownloaderService {
        return retrofit.create(SocialDownloaderService::class.java)
    }

   /* @Provides
    @SocialDownloaderBaseUrl
    fun provideSocialDownloaderBaseUrl(): String =
        "https://stage-ai-livewallpaper-backend.aspire.pics/"*/

}
