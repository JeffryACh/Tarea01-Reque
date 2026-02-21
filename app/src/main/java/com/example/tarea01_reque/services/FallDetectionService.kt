package com.example.tarea01_reque.services

import android.app.Notification
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

    // Variables de estado
    private var isCountingDown = false
    private var emergencyNumber: String = ""

    // Corrutinas para manejar la cuenta regresiva sin bloquear la app
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL_ALARM = "ACTION_CANCEL_ALARM"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "SafeWalkChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                emergencyNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
                startSurveillance()
            }
            ACTION_CANCEL_ALARM -> {
                cancelCountdown()
            }
            ACTION_STOP -> {
                stopSurveillance()
            }
        }
        return START_STICKY // Asegura que el servicio se reinicie si el sistema lo mata
    }

    private fun startSurveillance() {
        createNotificationChannel()
        val notification = buildNotification("Modo Vigilancia Activado", "Protegiéndote en segundo plano...")

        // Iniciar en primer plano para evitar las restricciones de batería de Samsung
        startForeground(NOTIFICATION_ID, notification)

        // Configurar el sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSurveillance() {
        sensorManager.unregisterListener(this)
        cancelCountdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && !isCountingDown) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Fórmula solicitada en el documento: magnitud del vector fuerza G
            val gForce = sqrt((x * x + y * y + z * z).toDouble())

            // Umbral de 30 m/s^2 exigido en la tarea
            if (gForce > 30.0) {
                triggerFallDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario implementar para el acelerómetro en este caso
    }

    private fun triggerFallDetected() {
        isCountingDown = true
        Log.d("SafeWalk", "¡Caída detectada! Iniciando cuenta regresiva...")

        // Actualizar notificación para alertar al usuario
        updateNotification("¡Caída Detectada!", "Enviando SMS en 10 segundos...")

        countdownJob = serviceScope.launch {
            // Cuenta regresiva de 10 segundos
            for (i in 10 downTo 1) {
                delay(1000)
                Log.d("SafeWalk", "Alerta en $i...")
            }

            // Si llegamos aquí y la corrutina no fue cancelada, se ejecuta la alarma
            executeEmergencyProtocol()
        }
    }

    private fun cancelCountdown() {
        if (isCountingDown) {
            countdownJob?.cancel()
            isCountingDown = false
            updateNotification("Modo Vigilancia Activado", "Falsa alarma cancelada. Todo en orden.")
            Log.d("SafeWalk", "Cuenta regresiva cancelada por el usuario.")
        }
    }

    private fun executeEmergencyProtocol() {
        isCountingDown = false

        // 1. Vibrar
        vibratePhone()

        // 2. Enviar SMS usando el hardware nativo
        sendSms()

        // Actualizar UI
        updateNotification("Alerta Enviada", "Se ha enviado un SMS al contacto de emergencia.")
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1500)
        }
    }

    private fun sendSms() {
        if (emergencyNumber.isNotEmpty()) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val message = "¡EMERGENCIA! El sistema SafeWalk ha detectado una posible caída o impacto. Por favor, contáctame de inmediato."
                smsManager.sendTextMessage(emergencyNumber, null, message, null, null)
                Log.d("SafeWalk", "SMS enviado exitosamente a $emergencyNumber")
            } catch (e: Exception) {
                Log.e("SafeWalk", "Error al enviar SMS: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Vigilancia SafeWalk",
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que se vea claramente
            ).apply {
                description = "Monitorea el acelerómetro en busca de caídas"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            // IMPORTANTE: Asegúrate de tener el ícono correcto en res/drawable,
            // puedes cambiar ic_launcher_foreground por un ícono tuyo
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}