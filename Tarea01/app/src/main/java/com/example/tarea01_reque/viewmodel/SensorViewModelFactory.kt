package com.example.tarea01_reque.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tarea01_reque.data.DataStoreManager

class SensorViewModelFactory(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(context, dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}