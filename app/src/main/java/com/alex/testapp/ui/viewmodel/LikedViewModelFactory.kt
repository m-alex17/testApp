package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase

class LikedViewModelFactory(
    private val getLikedVideosUseCase: GetLikedVideosUseCase,
    private val toggleLikeUseCase: ToggleVideoLikeUseCase,
    private val videoRepository: VideoRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LikeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LikeViewModel(toggleLikeUseCase, videoRepository, getLikedVideosUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}