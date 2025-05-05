package com.alex.testapp.data.repository

import com.alex.testapp.data.Video
import com.alex.testapp.data.local.VideoEntity
import com.alex.testapp.data.local.dao.VideoDao
import com.alex.testapp.data.remot.VideoApiService
import com.alex.testapp.data.remot.VideoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(
    private val videoDao: VideoDao,
    private val videoApiService: VideoApiService

) {

    suspend fun getVideos(): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getAllVideos().map { mapToVideo(it) }
    }

    suspend fun getVideoById(videoId: Int): Video? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(videoId)?.let { mapToVideo(it) }
    }

    private fun mapToVideo(entity: VideoEntity): Video {
        return Video(entity.id, entity.title, entity.url, entity.duration)
    }

    suspend fun fetchVideos(offset: Int): VideoResult {
        return videoApiService.getVideos(offset).result
    }

    suspend fun insertVideo(video: Video) {
        val entity = VideoEntity(
            id = video.id,
            title = video.title,
            url = video.url,
            duration = video.duration
        )

        videoDao.insertVideo(entity)
    }
}