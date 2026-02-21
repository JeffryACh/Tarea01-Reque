package com.example.tarea01_reque.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class GravitySensorHelper(context: Context) {

    // Inicializamos el gestor de sensores de Android
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Un flujo (Flow) que emite un valor 'true' cada vez que se detecta una caída.
     * Al usar callbackFlow, el listener del sensor solo se registra cuando alguien
     * está "escuchando" este flujo, y se destruye automáticamente cuando dejan de escuchar.
     */
    val fallDetectionFlow: Flow<Boolean> = callbackFlow {

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Cálculo Técnico: Magnitud del vector fuerza G
                    val gForce = sqrt((x * x + y * y + z * z).toDouble())

                    // Desafío: Si la magnitud supera los 30 m/s^2, emitimos una alerta
                    if (gForce > 30.0) {
                        trySend(true) // Emite la señal de alerta hacia el ViewModel
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitamos gestionar cambios de precisión para esta tarea
            }
        }

        // Registramos el listener
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // awaitClose se ejecuta automáticamente cuando el ViewModel o la UI
        // cancelan la corrutina (ej. la app se cierra por completo).
        // Esto previene fugas de memoria.
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}