package com.example.tarea01_reque.viewmodel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tarea01_reque.data.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class SensorViewModel(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel(), SensorEventListener {

    // Estados
    private val _isVigilanceMode = MutableStateFlow(false)
    val isVigilanceMode: StateFlow<Boolean> = _isVigilanceMode

    private val _isFallDetected = MutableStateFlow(false)
    val isFallDetected: StateFlow<Boolean> = _isFallDetected

    private val _emergencyPhone = MutableStateFlow("")
    val emergencyPhone: StateFlow<String> = _emergencyPhone

    private val _lastAcceleration = MutableStateFlow(Triple(0f, 0f, 0f))
    val lastAcceleration: StateFlow<Triple<Float, Float, Float>> = _lastAcceleration

    // Sensores
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Constantes
    private val FALL_THRESHOLD = 30f // m/s²
    private val VIBRATION_DURATION = 1000L // 1 segundo

    init {
        // Cargar teléfono guardado
        viewModelScope.launch {
            dataStoreManager.getPhone.collect { phone ->
                _emergencyPhone.value = phone
            }
        }
    }

    fun toggleVigilanceMode() {
        _isVigilanceMode.value = !_isVigilanceMode.value

        if (_isVigilanceMode.value) {
            startMonitoring()
            vibrate()
        } else {
            stopMonitoring()
        }
    }

    private fun startMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI // Balance entre precisión y batería
            )
        }
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
    }

    fun saveEmergencyContact(phone: String) {
        viewModelScope.launch {
            dataStoreManager.savePhone(phone)
        }
    }

    fun sendEmergencyAlert(
        onSendSMS: (String, String) -> Unit,
        onMakeCall: (String) -> Unit
    ) {
        val phone = _emergencyPhone.value
        if (phone.isNotEmpty()) {
            vibrate()

            // Mensaje de alerta
            val message = "¡ALERTA! SafeWalk ha detectado una posible emergencia. " +
                    "Por favor contacta al usuario."

            // Enviar SMS
            onSendSMS(phone, message)

            // También hacer una llamada (opcional)
            // onMakeCall(phone)
        }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_DURATION)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            _lastAcceleration.value = Triple(x, y, z)

            // Calcular magnitud
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Detectar caída (solo si modo vigilancia activo)
            if (magnitude > FALL_THRESHOLD && _isVigilanceMode.value) {
                _isFallDetected.value = true
                vibrate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesario
    }

    fun resetFallDetection() {
        _isFallDetected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}