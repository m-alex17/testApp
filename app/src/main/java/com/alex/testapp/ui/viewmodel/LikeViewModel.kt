package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.testapp.data.Video
import com.alex.testapp.data.VideoItem
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LikeViewModel(
    private val toggleLikeUseCase: ToggleVideoLikeUseCase,
    private val videoRepository: VideoRepository,
    private val getLikedVideosUseCase: GetLikedVideosUseCase

) : ViewModel() {

    private val _likedVideos = MutableStateFlow<List<Video>>(emptyList())
    val likedVideos: StateFlow<List<Video>> = _likedVideos.asStateFlow()

    fun toggleLike(video: Video) {
        viewModelScope.launch {
            videoRepository.insertVideo(video)
            val liked = toggleLikeUseCase.invoke(video.id)

            loadLikedVideos()
        }
    }

    fun loadLikedVideos() {
        viewModelScope.launch {
            val videos = getLikedVideosUseCase()
            _likedVideos.emit(videos)
        }
    }

}