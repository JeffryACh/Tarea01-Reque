package com.example.tarea01_reque.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tarea01_reque.viewmodel.SensorViewModel

@Composable
fun AlarmScreen(
    viewModel: SensorViewModel,
    onPanicClick: () -> Unit,
    onSaveContactClick: (String) -> Unit
) {
    val isVigilanceMode by viewModel.isVigilanceMode.collectAsState()
    val isFallDetected by viewModel.isFallDetected.collectAsState()
    val emergencyPhone by viewModel.emergencyPhone.collectAsState()
    val lastAcceleration by viewModel.lastAcceleration.collectAsState()

    var phoneInput by remember { mutableStateOf(emergencyPhone) }
    var showContactSaved by remember { mutableStateOf(false) }

    // Actualizar phoneInput cuando cambia emergencyPhone
    LaunchedEffect(emergencyPhone) {
        phoneInput = emergencyPhone
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título
        Text(
            text = "🚶 SafeWalk",
            fontSize = 36.sp,
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Tu centinela de bolsillo",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card para datos del sensor (debug)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Acelerómetro:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "X: %.2f  Y: %.2f  Z: %.2f".format(
                        lastAcceleration.first,
                        lastAcceleration.second,
                        lastAcceleration.third
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Campo para número de emergencia
        OutlinedTextField(
            value = phoneInput,
            onValueChange = {
                phoneInput = it
            },
            label = { Text("📞 Teléfono de emergencia") },
            placeholder = { Text("Ej: 5551234567") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneInput.isNotEmpty() && phoneInput.length < 10,
            supportingText = {
                if (phoneInput.isNotEmpty() && phoneInput.length < 10) {
                    Text(
                        text = "El número debe tener al menos 10 dígitos",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        // Botón para guardar contacto
        Button(
            onClick = {
                if (phoneInput.length >= 10) {
                    onSaveContactClick(phoneInput)
                    showContactSaved = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneInput.length >= 10
        ) {
            Text("💾 Guardar contacto")
        }

        // Mensaje de contacto guardado
        if (showContactSaved) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                showContactSaved = false
            }
            Text(
                text = "✓ Contacto guardado exitosamente",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Switch para modo vigilancia
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isVigilanceMode)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛡️ Modo Vigilancia",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isVigilanceMode) "Activado" else "Desactivado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isVigilanceMode)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isVigilanceMode,
                    onCheckedChange = { viewModel.toggleVigilanceMode() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de pánico
        Button(
            onClick = onPanicClick,
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isVigilanceMode)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            enabled = emergencyPhone.isNotEmpty()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚨",
                    fontSize = 40.sp
                )
                Text(
                    text = "¡PÁNICO!",
                    fontSize = 24.sp
                )
            }
        }

        if (emergencyPhone.isEmpty()) {
            Text(
                text = "⚠️ Guarda un contacto de emergencia primero",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        // Indicador de caída detectada
        if (isFallDetected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = "¡CAÍDA DETECTADA!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Enviando alerta en 10 segundos...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}