package com.alex.testapp.domain.manager

import com.alex.testapp.data.User
import com.alex.testapp.data.local.UserPreferences
import com.alex.testapp.data.repository.UserRepository
import com.alex.testapp.util.SingletonHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class UserManager private constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
    ) {

    private val _currentUserId = MutableStateFlow(1)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()


    suspend fun refreshCurrentUser() {
        val user = userRepository.getUserById(_currentUserId.value)
        _currentUser.value = user
    }

    suspend fun initialize() {
        val savedId = userPreferences.currentUserIdFlow.first()
        _currentUserId.value = savedId
        refreshCurrentUser()
    }

//    suspend fun loadUsers() {
//        val users = userRepository.getUsers()
//        _usersFlow.value = users
//    }

    suspend fun getUsers(): List<User> {
        return userRepository.getUsers()
    }

    suspend fun switchUser(userId: Int) {
        if (_currentUserId.value != userId) {
            _currentUserId.value = userId
            _currentUser.value = null
            userPreferences.saveUserId(userId)
            refreshCurrentUser()
        }
    }

    companion object : SingletonHolder<UserManager, Pair<UserRepository, UserPreferences>>({
        UserManager(it.first, it.second)
    })}