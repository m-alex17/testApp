package com.alex.testapp.data.repository

import com.alex.testapp.data.remot.AdvertiseResult
import com.alex.testapp.data.remot.VideoApiService

class AdvertiseRepository (
    private val advertiseApiService: VideoApiService.AdvertiseApiService

) {
    suspend fun fetchAdvertises(): AdvertiseResult {
        return advertiseApiService.getAdvertises().result
    }
}