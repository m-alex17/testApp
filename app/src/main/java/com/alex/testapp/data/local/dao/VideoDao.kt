package com.alex.testapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alex.testapp.data.local.VideoEntity

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    suspend fun getAllVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoById(videoId: Int): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideo(video: VideoEntity)
}