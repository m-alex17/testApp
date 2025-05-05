package com.alex.testapp.domain.usecase

import com.alex.testapp.data.local.UserPreferences
import com.alex.testapp.domain.manager.UserManager

class SwitchUserUseCase(
    private val userManager: UserManager,
    private val userPreferences: UserPreferences
    ) {

    suspend operator fun invoke(userId: Int) {
        userManager.switchUser(userId)
        userPreferences.saveUserId(userId)
        userManager.refreshCurrentUser()
    }
}