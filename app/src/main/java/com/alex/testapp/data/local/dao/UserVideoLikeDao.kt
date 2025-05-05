package com.alex.testapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alex.testapp.data.local.UserVideoLikeEntity
import com.alex.testapp.data.local.VideoEntity

@Dao
interface UserVideoLikeDao {
    @Query("SELECT * FROM user_video_likes WHERE userId = :userId")
    suspend fun getLikedVideoIdsByUser(userId: Int): List<UserVideoLikeEntity>

    @Query("SELECT videos.* FROM videos INNER JOIN user_video_likes ON videos.id = user_video_likes.videoId WHERE user_video_likes.userId = :userId")
    suspend fun getLikedVideosByUser(userId: Int): List<VideoEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM user_video_likes WHERE userId = :userId AND videoId = :videoId)")
    suspend fun isVideoLikedByUser(userId: Int, videoId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun likeVideo(likeEntity: UserVideoLikeEntity)

    @Query("DELETE FROM user_video_likes WHERE userId = :userId AND videoId = :videoId")
    suspend fun unlikeVideo(userId: Int, videoId: Int)
}