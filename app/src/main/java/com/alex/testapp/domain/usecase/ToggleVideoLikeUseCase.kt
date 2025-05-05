package com.alex.testapp.domain.usecase

import com.alex.testapp.data.repository.UserVideoLikeRepository
import com.alex.testapp.domain.manager.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToggleVideoLikeUseCase(
    private val likeRepository: UserVideoLikeRepository,
    private val userManager: UserManager
) {
    suspend operator fun invoke(videoId: Int): Boolean = withContext(Dispatchers.IO) {
        val userId = userManager.currentUserId.value
        return@withContext if (likeRepository.isVideoLiked(userId, videoId)) {
            likeRepository.unlikeVideo(userId, videoId)
            false
        } else {
            likeRepository.likeVideo(userId, videoId)
            true
        }
    }
}