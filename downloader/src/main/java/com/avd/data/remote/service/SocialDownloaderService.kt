package com.avd.data.remote.service

import android.system.Os.link
import com.avd.ui.main.home.downloadapi.SocialDownloaderResponse
import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface SocialDownloaderService {
    @POST
    @Headers("Content-Type: application/json")
    suspend fun downloadVideo(
        @Url endPoint: String,
        @Query("url") url: String
    ): Response<SocialDownloaderResponse>
}
