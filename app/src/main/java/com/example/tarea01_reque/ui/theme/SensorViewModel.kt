package com.example.tarea01_reque.ui.theme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tarea01_reque.data.DataStoreManager
import com.example.tarea01_reque.sensors.GravitySensorHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorViewModel(
    private val dataStoreManager: DataStoreManager,
    private val gravitySensorHelper: GravitySensorHelper
) : ViewModel() {

    private val _emergencyNumber = MutableStateFlow("")
    val emergencyNumber = _emergencyNumber.asStateFlow()

    private val _contactName = MutableStateFlow("")
    val contactName = _contactName.asStateFlow()

    private val _isSurveillanceActive = MutableStateFlow(false)
    val isSurveillanceActive = _isSurveillanceActive.asStateFlow()

    private val _isAlarmTriggered = MutableStateFlow(false)
    val isAlarmTriggered = _isAlarmTriggered.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.emergencyNumberFlow.collect { _emergencyNumber.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.contactNameFlow.collect { _contactName.value = it }
        }
        viewModelScope.launch {
            // Se conecta al flujo de caída del GravitySensorHelper
            gravitySensorHelper.fallDetectionFlow.collect { fell ->
                if (_isSurveillanceActive.value && fell && !_isAlarmTriggered.value) {
                    _isAlarmTriggered.value = true
                }
            }
        }
    }

    fun saveInfo(number: String, name: String) {
        viewModelScope.launch {
            // Sanitización: Remueve espacios, paréntesis y guiones para que el SMS no falle
            val cleanNumber = number.replace(Regex("[^0-9+]"), "")
            Log.d("SafeWalk", "Guardando - Original: $number -> Limpio: $cleanNumber")
            dataStoreManager.saveContactInfo(cleanNumber, name)
        }
    }

    fun toggleSurveillance(active: Boolean) { _isSurveillanceActive.value = active }
    fun setAlarmTriggered(triggered: Boolean) { _isAlarmTriggered.value = triggered }
}

class SensorViewModelFactory(
    private val ds: DataStoreManager,
    private val gs: GravitySensorHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SensorViewModel(ds, gs) as T
}