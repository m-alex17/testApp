package com.alex.testapp

import android.app.Application
import com.alex.testapp.data.local.AppDatabase
import com.alex.testapp.data.local.UserPreferences
import com.alex.testapp.data.repository.UserRepository
import com.alex.testapp.domain.manager.UserManager
import com.alex.testapp.util.AppInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApp : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
//        AppDatabase.getDatabase(this)
//        AppDatabase.populateInitialData(context = this, scope = applicationScope)

        val userPreferences = UserPreferences(this)
        val db = AppDatabase.getDatabase(this)
        val userRepository = UserRepository(db.userDao())

        val userManager = UserManager.getInstance(userRepository to userPreferences)

        applicationScope.launch {
            AppDatabase.populateInitialData(context = this@MyApp, scope = applicationScope)
            AppInitializer.isDataReady.await() // Wait until complete(true) is called
            userManager.initialize()
        }

    }
}