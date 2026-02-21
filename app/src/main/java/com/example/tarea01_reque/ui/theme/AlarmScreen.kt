package com.example.tarea01_reque.ui.theme // Asegúrate de que coincida con la ruta de tu paquete

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: SensorViewModel,
    onStartSurveillance: (String) -> Unit, // Callback para iniciar el servicio en MainActivity
    onStopSurveillance: () -> Unit,        // Callback para detener el servicio
    onCancelAlarm: () -> Unit              // Callback para detener la cuenta regresiva
) {
    // 1. Observar los estados del ViewModel
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()
    val isSurveillanceActive by viewModel.isSurveillanceActive.collectAsState()
    val isAlarmTriggered by viewModel.isAlarmTriggered.collectAsState()

    // Variable local para el TextField de ingreso de número
    var inputNumber by remember { mutableStateOf(emergencyNumber) }

    // Sincronizar el input con el DataStore cuando se cargue por primera vez
    LaunchedEffect(emergencyNumber) {
        if (inputNumber.isEmpty()) {
            inputNumber = emergencyNumber
        }
    }

    // 2. Animación de color de fondo reactiva (Fase 1: Cambios de color)
    val backgroundColor by animateColorAsState(
        targetValue = if (isAlarmTriggered) Color(0xFFD32F2F) else MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 500),
        label = "Background Color Animation"
    )

    val contentColor = if (isAlarmTriggered) Color.White else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // TÍTULO
        Text(
            text = "SafeWalk",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(top = 32.dp)
        )

        Text(
            text = "El Centinela de Bolsillo",
            fontSize = 16.sp,
            color = contentColor.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CONFIGURACIÓN DEL CONTACTO (Persistencia)
        OutlinedTextField(
            value = inputNumber,
            onValueChange = { inputNumber = it },
            label = { Text("Número de Emergencia") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            // --- ACTUALIZADO A MATERIAL 3 ---
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = 0.5f),
                focusedLabelColor = contentColor,
                unfocusedLabelColor = contentColor.copy(alpha = 0.7f)
            )
        )

        Button(
            onClick = { viewModel.saveEmergencyNumber(inputNumber) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Número")
        }

        // --- ACTUALIZADO A MATERIAL 3 ---
        HorizontalDivider(
            color = contentColor.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // INTERRUPTOR DE MODO VIGILANCIA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Modo Vigilancia",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
            Switch(
                checked = isSurveillanceActive,
                onCheckedChange = { isActive ->
                    viewModel.toggleSurveillance(isActive)
                    if (isActive) {
                        onStartSurveillance(emergencyNumber)
                    } else {
                        onStopSurveillance()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // BOTÓN GIGANTE DE PÁNICO / ESTADO DE ALARMA
        if (isAlarmTriggered) {
            // Si la alarma está disparada (por caída o por botón)
            Text(
                text = "¡ALERTA ACTIVADA!\nEnviando SMS...",
                color = contentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.setAlarmTriggered(false)
                    onCancelAlarm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("CANCELAR ALARMA", fontSize = 18.sp, color = Color.White)
            }
        } else {
            // Botón de Pánico Normal
            Button(
                onClick = {
                    viewModel.setAlarmTriggered(true)
                    onStartSurveillance(emergencyNumber) // Aseguramos que el servicio envíe el SMS
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Pánico",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "PÁNICO",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}