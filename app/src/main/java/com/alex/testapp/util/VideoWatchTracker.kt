package com.alex.testapp.util

import android.util.Log
import com.alex.testapp.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object  VideoWatchTracker {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var userRepository: UserRepository

    fun init(repo: UserRepository) {
        userRepository = repo

    }

    private val watchedCountFlows = ConcurrentHashMap<Int, MutableStateFlow<Int>>()

    // Track active watching sessions
    private val activeWatches = ConcurrentHashMap<Pair<Int, Int>, Long>()

    // In-memory tracking of videos already watched this session
    private val watchedVideos = ConcurrentHashMap<Int, MutableSet<Int>>()

    // Counter for watched videos per user in current session
    private val watchedCounters = ConcurrentHashMap<Int, AtomicInteger>()

    // Track cumulative watch time for each video (userId, videoId) -> cumulativeTimeInSeconds
    private val cumulativeWatchTime = ConcurrentHashMap<Pair<Int, Int>, AtomicInteger>()

    fun onVideoStarted(userId: Int, videoId: Int) {
        Log.e("VideoTracker", "onVideoStarted - User ID: $userId, Video ID: $videoId")

        // Initialize structures for this user if needed
        if (!watchedVideos.containsKey(userId)) {
            watchedVideos[userId] = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
            watchedCounters[userId] = AtomicInteger(0)
        }

        // Even if already watched, still track start time to add to cumulative time
        val key = Pair(userId, videoId)
        activeWatches[key] = System.currentTimeMillis()
    }

    fun onVideoStopped(userId: Int, videoId: Int) {
        Log.e("VideoTracker", "onVideoStopped - User ID: $userId, Video ID: $videoId")

        val key = Pair(userId, videoId)
        val startTime = activeWatches.remove(key) ?: return

        // Calculate this session's watch time
        val watchTimeMs = System.currentTimeMillis() - startTime
        val watchTimeSec = watchTimeMs / 1000

        // Add to cumulative time
        val cumulativeTime = cumulativeWatchTime.getOrPut(key) { AtomicInteger(0) }
        val newTotalTime = cumulativeTime.addAndGet(watchTimeSec.toInt())

        Log.e("VideoTracker", "Watch time for Video $videoId: $watchTimeSec seconds, cumulative: $newTotalTime seconds")

        // If not already watched and cumulative time is now ≥ 5 seconds, mark as watched
        if (!isVideoWatched(userId, videoId) && newTotalTime >= 5) {
            markVideoAsWatched(userId, videoId)
        }
    }

    // Check if a video is considered watched in this session
    fun isVideoWatched(userId: Int, videoId: Int): Boolean {
        return watchedVideos[userId]?.contains(videoId) == true
    }

    // Mark video as watched in memory
    private fun markVideoAsWatched(userId: Int, videoId: Int) {
        // Only mark as watched if not already watched
        if (isVideoWatched(userId, videoId)) return

        // Add to watched set
        watchedVideos.getOrPut(userId) {
            Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
        }.add(videoId)

        // Increment watch counter
        val newCount = watchedCounters.getOrPut(userId) { AtomicInteger(0) }.incrementAndGet()

        // Update StateFlow
        val flow = watchedCountFlows.getOrPut(userId) { MutableStateFlow(0) }
        flow.value = newCount
        Log.e("VideoTracker", "Video $videoId marked as watched for user $userId. Total watched: $newCount")
    }

    fun getWatchedCountFlow(userId: Int): StateFlow<Int> {
        return watchedCountFlows.getOrPut(userId) {
            MutableStateFlow(watchedCounters[userId]?.get() ?: 0)
        }
    }

    // Get current watched count for a user
    fun getWatchedCount(userId: Int): Int {
        return watchedCounters[userId]?.get() ?: 0
    }

    // Get cumulative watch time for a specific video
    fun getCumulativeWatchTime(userId: Int, videoId: Int): Int {
        return cumulativeWatchTime[Pair(userId, videoId)]?.get() ?: 0
    }

    // To be called when leaving fragment
    fun saveWatchData(userId: Int) {
        // Process any ongoing watches
        val ongoingWatches = HashMap(activeWatches)
        ongoingWatches.forEach { (key, startTime) ->
            val (watchUserId, videoId) = key
            if (watchUserId == userId) {
                // Calculate this session's watch time
                val watchTimeMs = System.currentTimeMillis() - startTime
                val watchTimeSec = watchTimeMs / 1000

                // Add to cumulative time
                val cumulativeTime = cumulativeWatchTime.getOrPut(key) { AtomicInteger(0) }
                val newTotalTime = cumulativeTime.addAndGet(watchTimeSec.toInt())

                // If not already watched and cumulative time is now ≥ 5 seconds, mark as watched
                if (!isVideoWatched(userId, videoId) && newTotalTime >= 5) {
                    markVideoAsWatched(userId, videoId)
                }

                // Remove this watch
                activeWatches.remove(key)
            }
        }

        // Persist the watch count to database
        val watchCount = getWatchedCount(userId)
        if (watchCount > 0) {
            coroutineScope.launch {
                try {
                    val user = userRepository.getUserById(userId)
                    user?.let {
                        userRepository.updateWatchedVideosCount(user.id,  watchCount)
                    }
                    Log.e("VideoTracker", "Saved watch count for user $userId: $watchCount")
                } catch (e: Exception) {
                    Log.e("VideoTracker", "Failed to save watch count", e)
                }
            }
        }
    }

    fun loadWatchCount(userId: Int) {
        coroutineScope.launch {
            try {
                val savedCount = userRepository.getWatchedVideosCountFlow(userId) ?: 0

                // Initialize in-memory structures
                watchedVideos.getOrPut(userId) { Collections.newSetFromMap(ConcurrentHashMap()) }
                watchedCounters[userId] = AtomicInteger(savedCount)
                watchedCountFlows[userId] = MutableStateFlow(savedCount)

                Log.e("VideoTracker", "Loaded watched count from DB for user $userId: $savedCount")
            } catch (e: Exception) {
                Log.e("VideoTracker", "Failed to load watched count for user $userId", e)
            }
        }

    }
}