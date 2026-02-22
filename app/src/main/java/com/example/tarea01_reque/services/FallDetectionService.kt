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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    // Corrutina para la cuenta regresiva de 10 segundos pedida en la Prueba de Campo
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null
    private var isAlarmTriggered = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL_ALARM = "ACTION_CANCEL_ALARM"
        const val ACTION_TRIGGER_PANIC = "ACTION_TRIGGER_PANIC" // <-- Nueva acción agregada
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
                startForegroundService()
                startSensor()
            }
            ACTION_TRIGGER_PANIC -> {
                emergencyNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                startForegroundService()
                // Si la alarma no se ha disparado ya, iniciamos la cuenta regresiva y el SMS
                if (!isAlarmTriggered) {
                    triggerFallCountdown()
                }
            }
            ACTION_STOP -> {
                stopSensor()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CANCEL_ALARM -> {
                cancelAlarm()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SafeWalk Activo")
            .setContentText("Modo vigilancia activado. Protegiéndote en segundo plano.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
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
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Cálculo Técnico de la Rúbrica: Magnitud de la fuerza G
            val gForce = sqrt((x * x + y * y + z * z).toDouble())

            // Desafío: Si supera los 30 m/s^2, se dispara la alerta de caída
            if (gForce > 30.0) {
                triggerFallCountdown()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Inicia la cuenta regresiva de 10 segundos antes de enviar el SMS
     */
    private fun triggerFallCountdown() {
        isAlarmTriggered = true
        Log.d("SafeWalk", "¡Emergencia detectada! Iniciando cuenta regresiva.")

        countdownJob = serviceScope.launch {
            delay(10000) // Espera 10 segundos
            executeEmergencyProtocol() // Si no se canceló en 10s, envía el SMS
        }
    }

    /**
     * Función llamada cuando el usuario presiona "Cancelar Alarma" en la interfaz
     */
    private fun cancelAlarm() {
        countdownJob?.cancel()
        countdownJob = null
        isAlarmTriggered = false
        Log.d("SafeWalk", "Alarma de SMS cancelada por el usuario.")
    }

    /**
     * Vibración del dispositivo y uso de SmsManager para despachar el mensaje.
     */
    private fun executeEmergencyProtocol() {
        vibratePhone()

        emergencyNumber?.let { number ->
            sendSms(number)
        }

        // Resetea para seguir vigilando después de enviar la alerta
        isAlarmTriggered = false
    }

    private fun vibratePhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(1500)
        }
    }

    private fun sendSms(phoneNumber: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val message = "¡EMERGENCIA! El usuario de SafeWalk ha detectado una caída peligrosa o requiere asistencia inmediata."

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SafeWalk", "SMS enviado con éxito a $phoneNumber")

        } catch (e: Exception) {
            Log.e("SafeWalk", "Error al enviar el SMS: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "SafeWalk Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}