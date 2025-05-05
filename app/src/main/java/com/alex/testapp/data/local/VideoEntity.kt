package com.alex.testapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val url: String,
    val duration: Double
)