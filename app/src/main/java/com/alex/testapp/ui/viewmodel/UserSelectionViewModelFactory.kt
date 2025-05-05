package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alex.testapp.domain.manager.UserManager
import com.alex.testapp.domain.usecase.SwitchUserUseCase

class UserSelectionViewModelFactory(
    private val userManager: UserManager,
    private val switchUserUseCase: SwitchUserUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserSelectionViewModel(userManager, switchUserUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}