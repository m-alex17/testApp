package com.alex.testapp.domain.usecase

import com.alex.testapp.data.repository.UserVideoLikeRepository
import com.alex.testapp.domain.manager.UserManager

class IsVideoLikedUseCase(
    private val likeRepository: UserVideoLikeRepository,
    private val userManager: UserManager
) {

    suspend operator fun invoke(videoId: Int): Boolean {
        return likeRepository.isVideoLiked(userManager.currentUserId.value, videoId)
    }
}