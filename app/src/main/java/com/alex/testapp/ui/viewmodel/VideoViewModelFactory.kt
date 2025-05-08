package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.GetVideosUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase


class VideoViewModelFactory(
    private val getVideosUseCase: GetVideosUseCase,
    private val toggleLikeUseCase: ToggleVideoLikeUseCase,
    private val videoRepository: VideoRepository,
    private val getLikedVideosUseCase: GetLikedVideosUseCase,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(getVideosUseCase, toggleLikeUseCase, videoRepository, getLikedVideosUseCase,) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
