package com.alex.testapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.testapp.data.remot.Advertise
import com.alex.testapp.data.repository.AdvertiseRepository
import kotlinx.coroutines.launch

class AdvertiseViewModel(
    private val repository: AdvertiseRepository
) : ViewModel() {
    private val _advertises = MutableLiveData<List<Advertise>>()
    val advertises: LiveData<List<Advertise>> get() = _advertises

    init {
        loadAdvertises()
    }

    private fun loadAdvertises(){
        viewModelScope.launch {
            try {
                val result = repository.fetchAdvertises()
                _advertises.value = result.advertises
            } catch (e: Exception) {
                //todo send error message to ui
            } finally {
            }
        }
    }
}