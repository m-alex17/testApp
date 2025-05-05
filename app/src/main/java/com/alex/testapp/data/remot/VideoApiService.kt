package com.alex.testapp.data.remot

import retrofit2.http.Query
import retrofit2.http.GET

interface VideoApiService {
    @GET("videos/index")
    suspend fun getVideos(
        @Query("offset") offset: Int
    ): VideoResponse
}