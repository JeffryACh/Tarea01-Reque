package com.example.tarea01_reque

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.tarea01_reque.data.DataStoreManager
import com.example.tarea01_reque.sensors.GravitySensorHelper
import com.example.tarea01_reque.services.FallDetectionService
import com.example.tarea01_reque.ui.theme.AlarmScreen
import com.example.tarea01_reque.ui.theme.SensorViewModel
import com.example.tarea01_reque.ui.theme.SensorViewModelFactory

class MainActivity : ComponentActivity() {

    // 1. Inicialización diferida (lazy) de nuestras dependencias base
    private val dataStoreManager by lazy { DataStoreManager(applicationContext) }
    private val gravitySensorHelper by lazy { GravitySensorHelper(applicationContext) }

    // 2. Inicialización del ViewModel usando el Factory que creamos
    private val viewModel: SensorViewModel by viewModels {
        SensorViewModelFactory(dataStoreManager, gravitySensorHelper)
    }

    // 3. Lanzador para pedir permisos en tiempo de ejecución (Fase 3 del proyecto)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        if (!smsGranted) {
            Toast.makeText(this, "El permiso de SMS es necesario para enviar alertas.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedimos los permisos críticos al iniciar la aplicación
        requestPermissions()

        setContent {
            // Usamos el tema estándar de Material 3
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AlarmScreen(
                        viewModel = viewModel,
                        onStartSurveillance = { emergencyNumber ->
                            startFallDetectionService(emergencyNumber)
                        },
                        onStopSurveillance = {
                            stopFallDetectionService()
                        },
                        onCancelAlarm = {
                            cancelFallAlarm()
                        },
                        // NUEVO: Callback específico para el botón de pánico manual
                        onTriggerPanic = { emergencyNumber ->
                            triggerPanicAlarm(emergencyNumber)
                        }
                    )
                }
            }
        }
    }

    /**
     * Solicita permisos de envío de SMS y de Notificaciones (Obligatorio en Android 13+ para servicios)
     */
    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf(Manifest.permission.SEND_SMS)

        // Si el dispositivo tiene Android 13 (Tiramisu) o superior,
        // necesitamos permiso para mostrar la notificación persistente del servicio.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    /**
     * Inicia el servicio en segundo plano que mantiene el sensor vivo (Modo Vigilancia)
     */
    private fun startFallDetectionService(emergencyNumber: String) {
        if (emergencyNumber.isBlank()) {
            Toast.makeText(this, "Por favor, guarda un número de emergencia primero.", Toast.LENGTH_SHORT).show()
            viewModel.toggleSurveillance(false) // Desactiva el switch si no hay número
            return
        }

        val serviceIntent = Intent(this, FallDetectionService::class.java).apply {
            action = FallDetectionService.ACTION_START
            putExtra(FallDetectionService.EXTRA_PHONE_NUMBER, emergencyNumber)
        }

        // Iniciar como Foreground Service en versiones modernas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * NUEVO: Dispara explícitamente la alarma y la cuenta regresiva desde el botón de pánico
     */
    private fun triggerPanicAlarm(emergencyNumber: String) {
        if (emergencyNumber.isBlank()) {
            Toast.makeText(this, "Por favor, guarda un número de emergencia primero.", Toast.LENGTH_SHORT).show()
            viewModel.setAlarmTriggered(false) // Resetea la pantalla si no hay número
            return
        }

        val serviceIntent = Intent(this, FallDetectionService::class.java).apply {
            action = FallDetectionService.ACTION_TRIGGER_PANIC
            putExtra(FallDetectionService.EXTRA_PHONE_NUMBER, emergencyNumber)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * Detiene la vigilancia en segundo plano
     */
    private fun stopFallDetectionService() {
        val serviceIntent = Intent(this, FallDetectionService::class.java).apply {
            action = FallDetectionService.ACTION_STOP
        }
        startService(serviceIntent)
    }

    /**
     * Envía una señal al servicio para detener la cuenta regresiva de 10 segundos
     */
    private fun cancelFallAlarm() {
        val serviceIntent = Intent(this, FallDetectionService::class.java).apply {
            action = FallDetectionService.ACTION_CANCEL_ALARM
        }
        startService(serviceIntent)
    }
}