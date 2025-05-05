package com.alex.testapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val USER_ID_KEY = intPreferencesKey("user_id")
    }

    val currentUserIdFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[USER_ID_KEY] ?: 1
    }

    suspend fun saveUserId(userId: Int) {
        dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }
}
