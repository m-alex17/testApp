package com.alex.testapp.util

import kotlinx.coroutines.CompletableDeferred

object AppInitializer {
    val isDataReady = CompletableDeferred<Boolean>()
}