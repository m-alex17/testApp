package com.alex.testapp.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdvertiseTimeWatcher(
    private val targetSeconds: Int,
    private val timeProvider: () -> Int,
    private val onTimeReached: () -> Unit
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val currentTime = timeProvider()
                if (currentTime >= targetSeconds) {
                    onTimeReached()
                    cancel()
                }
                delay(500L)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}

