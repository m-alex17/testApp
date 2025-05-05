package com.alex.testapp.data.remot

import com.alex.testapp.data.Video

data class VideoResult(
    val videos: List<Video>,
    val remaining: Int,
    val end: Boolean
)