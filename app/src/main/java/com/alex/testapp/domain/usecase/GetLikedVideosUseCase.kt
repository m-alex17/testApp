package com.alex.testapp.domain.usecase

import com.alex.testapp.data.Video
import com.alex.testapp.data.repository.UserVideoLikeRepository
import com.alex.testapp.domain.manager.UserManager

class GetLikedVideosUseCase(
    private val likeRepository: UserVideoLikeRepository,
    private val userManager: UserManager
) {

    suspend operator fun invoke(): List<Video> {
        return likeRepository.getLikedVideos(userManager.currentUserId.value)
    }
}