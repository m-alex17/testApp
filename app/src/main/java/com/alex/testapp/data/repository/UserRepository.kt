package com.alex.testapp.data.repository

import com.alex.testapp.data.User
import com.alex.testapp.data.local.UserEntity
import com.alex.testapp.data.local.dao.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        userDao.getAllUsers().map { mapToUser(it) }
    }

    suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)?.let { mapToUser(it) }
    }

    private fun mapToUser(entity: UserEntity): User {
        return User(entity.id, entity.name)
    }

    suspend fun updateWatchedVideosCount(userId: Int, count: Int) {
        val user = userDao.getUserById(userId)

        user?.let {
            userDao.updateUser(it.copy(watchedVideosCount = count))
        }
    }

    fun getWatchedVideosCountFlow(userId: Int): Int? {
        return userDao.getWatchedVideosCountFlow(userId)
    }
}