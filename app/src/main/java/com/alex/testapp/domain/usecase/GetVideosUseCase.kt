package com.alex.testapp.domain.usecase

import com.alex.testapp.data.Video
import com.alex.testapp.data.remot.VideoResult
import com.alex.testapp.data.repository.VideoRepository

class GetVideosUseCase(private val videoRepository: VideoRepository) {

    suspend operator fun invoke(): List<Video> {
        return videoRepository.getVideos()
    }

    suspend operator fun invoke(offset: Int): VideoResult {
        return videoRepository.fetchVideos(offset)
    }
}
