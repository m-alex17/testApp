package com.alex.testapp.data.remot

import retrofit2.http.GET
import retrofit2.http.Query

interface VideoApiService {
    @GET("videos/index")
    suspend fun getVideos(
        @Query("offset") offset: Int
    ): VideoResponse


    interface AdvertiseApiService {
        @GET("advertises/index")
        suspend fun getAdvertises(): AdvertiseResponse
    }

}