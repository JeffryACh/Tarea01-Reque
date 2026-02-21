package com.example.tarea01_reque.ui.theme // Ajusta el paquete según tu estructura

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

    // 1. ESTADO: Número de Emergencia
    private val _emergencyNumber = MutableStateFlow("")
    val emergencyNumber: StateFlow<String> = _emergencyNumber.asStateFlow()

    // 2. ESTADO: Modo Vigilancia (Switch on/off)
    private val _isSurveillanceActive = MutableStateFlow(false)
    val isSurveillanceActive: StateFlow<Boolean> = _isSurveillanceActive.asStateFlow()

    // 3. ESTADO: Alarma Disparada (para cambiar el color de la pantalla)
    private val _isAlarmTriggered = MutableStateFlow(false)
    val isAlarmTriggered: StateFlow<Boolean> = _isAlarmTriggered.asStateFlow()

    init {
        // Al inicializarse, comenzamos a escuchar el DataStore automáticamente
        viewModelScope.launch {
            dataStoreManager.emergencyNumberFlow.collect { number ->
                _emergencyNumber.value = number
            }
        }

        // Escuchamos el sensor de hardware en tiempo real
        viewModelScope.launch {
            gravitySensorHelper.fallDetectionFlow.collect { fallDetected ->
                // Si detecta caída y el modo vigilancia está activo, cambiamos el estado de la UI
                if (fallDetected && _isSurveillanceActive.value) {
                    _isAlarmTriggered.value = true
                }
            }
        }
    }

    /**
     * Guarda el número en la persistencia local.
     */
    fun saveEmergencyNumber(number: String) {
        viewModelScope.launch {
            dataStoreManager.saveEmergencyNumber(number)
        }
    }

    /**
     * Activa o desactiva el Modo Vigilancia en la interfaz.
     */
    fun toggleSurveillance(isActive: Boolean) {
        _isSurveillanceActive.value = isActive
        if (!isActive) {
            // Si apagamos la vigilancia, reseteamos la alarma visual
            _isAlarmTriggered.value = false
        }
    }

    /**
     * Simula el botón de pánico manual o resetea la alarma visual.
     */
    fun setAlarmTriggered(isTriggered: Boolean) {
        _isAlarmTriggered.value = isTriggered
    }
}

/**
 * Como el ViewModel recibe parámetros en su constructor (DataStore y SensorHelper),
 * Android necesita un "Factory" (Fábrica) para saber cómo instanciarlo.
 */
class SensorViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val gravitySensorHelper: GravitySensorHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(dataStoreManager, gravitySensorHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}