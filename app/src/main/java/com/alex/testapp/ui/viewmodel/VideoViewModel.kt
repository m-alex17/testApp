package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.testapp.data.Video
import com.alex.testapp.data.repository.VideoRepository
import com.alex.testapp.domain.usecase.GetLikedVideosUseCase
import com.alex.testapp.domain.usecase.GetVideosUseCase
import com.alex.testapp.domain.usecase.ToggleVideoLikeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoViewModel(
    private val getVideosUseCase: GetVideosUseCase,
    private val toggleLikeUseCase: ToggleVideoLikeUseCase,
    private val videoRepository: VideoRepository,
    private val getLikedVideosUseCase: GetLikedVideosUseCase,
) : ViewModel() {

    private val _videoList = MutableStateFlow<List<Video>>(emptyList())
    val videoList: StateFlow<List<Video>> = _videoList.asStateFlow()

    private val _likedVideoIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedVideoIds: StateFlow<Set<Int>> = _likedVideoIds


    private var offset = 0
    private var isLoading = false
    private var endReached = false

    fun loadNextVideos() {
        if (isLoading || endReached) return

        viewModelScope.launch {
            try {
                isLoading = true
                val result = getVideosUseCase(offset)
                _videoList.update { current -> current + result.videos }
                offset += result.videos.size
                endReached = result.end
                isLoading = false
            } catch (e: Exception) {
                //todo send error message to ui
            } finally {
                isLoading = false
            }
        }
    }

    fun isEndReached() = endReached

    fun toggleLike(video: Video) {
        viewModelScope.launch {
            videoRepository.insertVideo(video)
            val liked = toggleLikeUseCase.invoke(video.id)
        }
    }

    fun loadLikedVideos() {
        viewModelScope.launch {
            val likedVideos = getLikedVideosUseCase()
            _likedVideoIds.value = likedVideos.map { it.id }.toSet()
        }
    }

    suspend fun getLatestLikedVideoIds(): Set<Int> {
            val likedVideos = getLikedVideosUseCase()
            return likedVideos.map { it.id }.toSet()
    }

}
