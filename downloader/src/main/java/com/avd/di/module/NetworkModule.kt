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
    private const val SOCIAL_DOWNLOADER_LOG_TAG = "SocialDownloaderHTTP"

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
                    .addHeader("x-rapidapi-key", "396f7824p121f56jsnfdcae46a09baf2cmsh7ba6df8f2dee4c0peeaf742abe7e0fp19a49ajsn9e4")
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
    fun provideBaseUrl1(): String = "https://backend-video-downloader.aspire.pics/"

    @BaseUrl2
    @Provides
    fun provideBaseUrl2(): String = "https://backend-video-downloader.aspire.pics/"

    @Provides
    @Named("SocialDownloaderKey")
    fun provideSocialDownloaderKey(): String = RemoteConfigHelper.getSocialDownloaderApiKey()


   @Provides
    @SocialDownloaderBaseUrl
    fun provideSocialDownloaderBaseUrl(): String =
        RemoteConfigHelper.getSocialDownloaderBaseUrl()

    @Provides
    @Singleton
    @Named("SocialDownloaderClient")
    fun provideSocialDownloaderOkHttpClient(@Named("SocialDownloaderKey") apiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("api-key", apiKey)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("X-App-Label", RemoteConfigHelper.getSocialDownloaderAppLabel())
                    .build()
                Log.d(
                    SOCIAL_DOWNLOADER_LOG_TAG,
                    "Request ${request.method} ${request.url} keySource=Hilt:SocialDownloaderKey apiKey=${apiKey.maskForLog()} keyLength=${apiKey.length} appLabel=${RemoteConfigHelper.getSocialDownloaderAppLabel()} authHeaders=${request.headers.names().filter { it.equals("api-key", true) || it.equals("x-api-key", true) || it.equals("X-App-Label", true) }}"
                )
                val response = chain.proceed(request)
                Log.d(
                    SOCIAL_DOWNLOADER_LOG_TAG,
                    "Response code=${response.code} message=${response.message} requestUrl=${request.url}"
                )
                response
            }
            .connectTimeout(90, TimeUnit.SECONDS) // increase connect timeout
            .readTimeout(90, TimeUnit.SECONDS)    // increase read timeout
            .writeTimeout(90, TimeUnit.SECONDS)   // increase write timeout
            .build()
    }

    private fun String.maskForLog(): String {
        if (isBlank()) return "<empty>"
        return if (length <= 10) "***" else "${take(4)}...${takeLast(4)}"
    }

    @SocialDownloaderBaseUrl
    @Provides
    @Singleton
    fun provideSocialDownloaderRetrofit(
        @Named("SocialDownloaderClient") okHttpClient: OkHttpClient,
        @SocialDownloaderBaseUrl baseUrl: String
    ): Retrofit {
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
