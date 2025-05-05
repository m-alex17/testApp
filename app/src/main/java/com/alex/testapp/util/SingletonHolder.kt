package com.alex.testapp.util

/**
 * Generic singleton holder pattern for thread-safe singleton instantiation
 */
open class SingletonHolder<out T, in A>(private val creator: (A) -> T) {
    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        return instance ?: synchronized(this) {
            instance ?: creator(arg).also { instance = it }
        }
    }
}