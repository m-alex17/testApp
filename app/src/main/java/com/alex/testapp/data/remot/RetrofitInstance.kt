package com.alex.testapp.data.remot

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://sandbox.innova-co.com/api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val videoApiService: VideoApiService by lazy {
        retrofit.create(VideoApiService::class.java)
    }
}
