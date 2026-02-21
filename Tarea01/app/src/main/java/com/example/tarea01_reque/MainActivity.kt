package com.example.tarea01_reque

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tarea01_reque.data.DataStoreManager
import com.example.tarea01_reque.ui.screens.AlarmScreen
import com.example.tarea01_reque.ui.theme.T1_Android_requeTheme
import com.example.tarea01_reque.viewmodel.SensorViewModel
import com.example.tarea01_reque.viewmodel.SensorViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var dataStoreManager: DataStoreManager

    // Registrar para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.SEND_SMS, false) -> {
                Toast.makeText(this, "Permiso de SMS concedido", Toast.LENGTH_SHORT).show()
            }
            permissions.getOrDefault(Manifest.permission.CALL_PHONE, false) -> {
                Toast.makeText(this, "Permiso de llamada concedido", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar DataStoreManager
        dataStoreManager = DataStoreManager(this)

        // Solicitar permisos
        requestPermissions()

        setContent {
            T1_Android_requeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Crear ViewModel con factory
                    val viewModel: SensorViewModel = viewModel(
                        factory = SensorViewModelFactory(this, dataStoreManager)
                    )

                    // Observar detección de caída
                    val isFallDetected by viewModel.isFallDetected.collectAsState()

                    // Manejar la caída detectada
                    LaunchedEffect(isFallDetected) {
                        if (isFallDetected) {
                            // Iniciar cuenta regresiva de 10 segundos
                            for (i in 10 downTo 1) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Alerta en $i segundos...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                delay(1000)
                            }

                            // Enviar alerta
                            viewModel.sendEmergencyAlert(
                                onSendSMS = { phone, message ->
                                    sendSMS(phone, message)
                                },
                                onMakeCall = { phone ->
                                    makeCall(phone)
                                }
                            )

                            viewModel.resetFallDetection()
                        }
                    }

                    AlarmScreen(
                        viewModel = viewModel,
                        onPanicClick = {
                            // Botón de pánico manual
                            viewModel.sendEmergencyAlert(
                                onSendSMS = { phone, message ->
                                    sendSMS(phone, message)
                                },
                                onMakeCall = { phone ->
                                    makeCall(phone)
                                }
                            )
                        },
                        onSaveContactClick = { phone ->
                            // Guardar contacto
                            viewModel.saveEmergencyContact(phone)
                        }
                    )
                }
            }
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {

                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)

                Toast.makeText(this, "SMS enviado a $phoneNumber", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permiso de SMS no concedido", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al enviar SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCall(phoneNumber: String) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {

                // Crear intent para llamada
                val intent = android.content.Intent(android.content.Intent.ACTION_CALL)
                intent.data = android.net.Uri.parse("tel:$phoneNumber")
                startActivity(intent)

            } else {
                Toast.makeText(this, "Permiso de llamada no concedido", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al llamar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}