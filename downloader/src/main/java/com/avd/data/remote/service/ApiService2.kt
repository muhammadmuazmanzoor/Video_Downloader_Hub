package com.avd.data.remote.service

import com.avd.ui.main.home.downloadapi.TikTokApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService2 {

    @GET("analysis")
    suspend fun executeApiRequest(
        @Query("url") url: String  // Field parameter for form data
    ): Response<TikTokApiResponse>  // Expected response

}