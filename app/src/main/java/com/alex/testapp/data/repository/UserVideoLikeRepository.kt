package com.alex.testapp.data.repository

import com.alex.testapp.data.Video
import com.alex.testapp.data.local.UserVideoLikeEntity
import com.alex.testapp.data.local.dao.UserVideoLikeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserVideoLikeRepository(private val userVideoLikeDao: UserVideoLikeDao) {

    suspend fun getLikedVideos(userId: Int): List<Video> = withContext(Dispatchers.IO) {
        userVideoLikeDao.getLikedVideosByUser(userId).map {
            Video(it.id, it.title, it.url, it.duration)
        }
    }

    suspend fun isVideoLiked(userId: Int, videoId: Int): Boolean = withContext(Dispatchers.IO) {
        userVideoLikeDao.isVideoLikedByUser(userId, videoId)
    }

    suspend fun likeVideo(userId: Int, videoId: Int) {
        userVideoLikeDao.likeVideo(UserVideoLikeEntity(userId, videoId))

    }
    suspend fun unlikeVideo(userId: Int, videoId: Int) {
        userVideoLikeDao.unlikeVideo(userId, videoId)
    }

}