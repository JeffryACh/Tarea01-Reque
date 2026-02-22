package com.example.tarea01_reque.services

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        const val ACTION_TRIGGER_PANIC = "ACTION_TRIGGER_PANIC"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"

        private const val NOTIFICATION_CHANNEL_ID = "SafeWalkChannel"
        private const val NOTIFICATION_ID = 1
        private const val SMS_SENT_ACTION = "SMS_SENT"
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

            // Cálculo Técnico: Magnitud de la fuerza G (Requisito Fase 2)
            val gForce = sqrt((x * x + y * y + z * z).toDouble())

            // Desafío: Si supera los 30 m/s^2, se dispara la alerta de caída
            if (gForce > 30.0) {
                triggerFallCountdown()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerFallCountdown() {
        isAlarmTriggered = true
        Log.d("SafeWalk", "¡Emergencia detectada! Iniciando cuenta regresiva.")

        countdownJob = serviceScope.launch {
            delay(10000) // Cuenta regresiva de 10 segundos (Prueba de Campo)
            executeEmergencyProtocol()
        }
    }

    private fun cancelAlarm() {
        countdownJob?.cancel()
        countdownJob = null
        isAlarmTriggered = false
        Log.d("SafeWalk", "Alarma de SMS cancelada por el usuario.")
    }

    private fun executeEmergencyProtocol() {
        vibratePhone()

        emergencyNumber?.let { number ->
            sendSms(number)
        }

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

    /**
     * Función mejorada: Monitorea el estado real del envío del SMS
     */
    private fun sendSms(phoneNumber: String) {
        val sentPI = PendingIntent.getBroadcast(
            this,
            0,
            Intent(SMS_SENT_ACTION),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Registrar receptor para capturar el resultado real de la operadora
        val sentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        Log.d("SafeWalk", "¡SMS ENTREGADO A LA RED!")
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                        Log.e("SafeWalk", "Fallo Genérico (Probablemente falta de saldo)")
                    SmsManager.RESULT_ERROR_NO_SERVICE ->
                        Log.e("SafeWalk", "Sin señal de celular")
                    SmsManager.RESULT_ERROR_NULL_PDU ->
                        Log.e("SafeWalk", "Error en el formato del mensaje")
                    SmsManager.RESULT_ERROR_RADIO_OFF ->
                        Log.e("SafeWalk", "Modo avión activado")
                }
                // Desregistrar para evitar fugas de memoria
                try {
                    context?.unregisterReceiver(this)
                } catch (e: Exception) {
                    Log.e("SafeWalk", "Error al desregistrar: ${e.message}")
                }
            }
        }

        // Compatibilidad con Android 14+ para receptores dinámicos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION))
        }

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val message = "¡EMERGENCIA! El usuario de SafeWalk ha detectado una caída peligrosa o requiere asistencia inmediata."

            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null)

        } catch (e: Exception) {
            Log.e("SafeWalk", "Error al intentar despachar: ${e.message}")
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