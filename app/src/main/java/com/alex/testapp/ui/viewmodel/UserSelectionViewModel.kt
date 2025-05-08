package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alex.testapp.data.User
import com.alex.testapp.domain.manager.UserManager
import com.alex.testapp.domain.usecase.SwitchUserUseCase
import kotlinx.coroutines.launch

class UserSelectionViewModel(
    private val userManager: UserManager,
    private val switchUserUseCase: SwitchUserUseCase
) : ViewModel() {

    private val _selectedUser = MutableLiveData<User?>()
    val selectedUser: LiveData<User?> = _selectedUser

    private val _bottomSheetDismissed = MutableLiveData<Boolean>()
    val bottomSheetDismissed: LiveData<Boolean> = _bottomSheetDismissed

    val currentUserId = userManager.currentUserId.asLiveData()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users


    fun selectUser(user: User) {
        _selectedUser.value = user
    }

    fun onBottomSheetDismissed() {
        _bottomSheetDismissed.value = true
    }

    fun onUserSelected(userId: Int) {
        viewModelScope.launch {
            if (userId != userManager.currentUserId.value) {
                switchUserUseCase.invoke(userId)
            }
        }
    }
}