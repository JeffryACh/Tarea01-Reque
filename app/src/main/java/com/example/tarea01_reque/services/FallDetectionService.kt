package com.example.tarea01_reque.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var emergencyNumber: String? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null
    private var isAlarmTriggered = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL_ALARM = "ACTION_CANCEL_ALARM"
        const val ACTION_TRIGGER_PANIC = "ACTION_TRIGGER_PANIC"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"

        private const val NOTIFICATION_CHANNEL_ID = "SafeWalkChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                emergencyNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                startForeground(NOTIFICATION_ID, createNotification())
                startSensor()
            }
            ACTION_TRIGGER_PANIC -> {
                emergencyNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                startForeground(NOTIFICATION_ID, createNotification())
                if (!isAlarmTriggered) triggerFallCountdown()
            }
            ACTION_STOP -> {
                stopSensor()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CANCEL_ALARM -> cancelAlarm()
        }
        return START_STICKY
    }

    private fun startSensor() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(this)
        cancelAlarm()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && !isAlarmTriggered) {
            val gForce = sqrt(event.values.map { it * it }.sum().toDouble())
            if (gForce > 30.0) { // Umbral técnico del PDF
                triggerFallCountdown()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerFallCountdown() {
        isAlarmTriggered = true
        Log.d("SafeWalk", "¡Emergencia! Iniciando cuenta regresiva de 10s.")
        countdownJob = serviceScope.launch {
            delay(10000)
            executeEmergencyProtocol()
        }
    }

    private fun cancelAlarm() {
        countdownJob?.cancel()
        isAlarmTriggered = false
        Log.d("SafeWalk", "Envío de SMS cancelado.")
    }

    private fun executeEmergencyProtocol() {
        emergencyNumber?.let { sendSms(it) }
        isAlarmTriggered = false
    }

    /**
     * Envío de SMS inspirado en el video
     * pero optimizado para evitar el "Fallo Genérico" (Problemas 3 y 5)
     */
    private fun sendSms(phoneNumber: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val message = "¡EMERGENCIA! Usuario de SafeWalk requiere asistencia inmediata."

            // Solución al problema 5: Dividir el mensaje si es complejo
            val parts = smsManager.divideMessage(message)

            // Envío directo (similar a null, null del video pero más robusto)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

            Log.d("SafeWalk", "SMS despachado hacia la red para el número: $phoneNumber")

        } catch (e: Exception) {
            Log.e("SafeWalk", "Error crítico en sendSms: ${e.message}")
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("SafeWalk Vigilando")
        .setContentText("Protección activa en segundo plano.")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "SafeWalk", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}