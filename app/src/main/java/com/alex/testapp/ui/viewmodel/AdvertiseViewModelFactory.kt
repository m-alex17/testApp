package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alex.testapp.data.repository.AdvertiseRepository

class AdvertiseViewModelFactory(
    private val repository: AdvertiseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvertiseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdvertiseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}