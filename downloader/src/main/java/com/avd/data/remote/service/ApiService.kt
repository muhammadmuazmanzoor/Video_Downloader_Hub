package com.avd.data.remote.service

import com.avd.ui.main.home.downloadapi.VideoResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface ApiService {
    @FormUrlEncoded  // Key annotation for form data
    @POST("download")
    suspend fun executeApiRequest(
        @Field("url") url: String  // Field parameter for form data
    ): Response<VideoResponse>  // Expected response
}
