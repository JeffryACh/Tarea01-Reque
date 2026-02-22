package com.example.tarea01_reque.ui.theme

import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: SensorViewModel,
    onStartSurveillance: (String) -> Unit,
    onStopSurveillance: () -> Unit,
    onCancelAlarm: () -> Unit,
    onTriggerPanic: (String) -> Unit // Callback para activar la cuenta regresiva del SMS
) {
    val context = LocalContext.current

    // Observamos los estados del ViewModel (incluyendo el nuevo nombre del contacto)
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()
    val contactName by viewModel.contactName.collectAsState()
    val isSurveillanceActive by viewModel.isSurveillanceActive.collectAsState()
    val isAlarmTriggered by viewModel.isAlarmTriggered.collectAsState()

    // Estados locales para los campos de texto
    var inputNumber by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }

    // Sincronización inicial con la base de datos (DataStore)
    LaunchedEffect(emergencyNumber, contactName) {
        inputNumber = emergencyNumber
        inputName = contactName
    }

    // Animación de color de fondo: Blanco a Rojo en emergencia
    val backgroundColor by animateColorAsState(
        targetValue = if (isAlarmTriggered) Color(0xFFD32F2F) else MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 500),
        label = "BgAnimation"
    )

    val contentColor = if (isAlarmTriggered) Color.White else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SafeWalk",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // CAMPO: NOMBRE DEL CONTACTO (Requisito Fase 4)
        OutlinedTextField(
            value = inputName,
            onValueChange = { inputName = it },
            label = { Text("Nombre del Contacto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = 0.5f)
            )
        )

        // CAMPO: NÚMERO (Sugerencia de formato para evitar fallos de red)
        OutlinedTextField(
            value = inputNumber,
            onValueChange = { inputNumber = it },
            label = { Text("Número (Ej: +506 8888 8888)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = 0.5f)
            )
        )

        Button(
            onClick = {
                viewModel.saveInfo(inputNumber, inputName)
                Toast.makeText(context, "Configuración guardada", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Configuración")
        }

        HorizontalDivider(color = contentColor.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // MODO VIGILANCIA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Modo Vigilancia", fontSize = 18.sp, color = contentColor)
            Switch(
                checked = isSurveillanceActive,
                onCheckedChange = { isActive ->
                    viewModel.toggleSurveillance(isActive)
                    if (isActive) onStartSurveillance(emergencyNumber) else onStopSurveillance()
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // LÓGICA DE ALERTA
        if (isAlarmTriggered) {
            Text(
                text = "ALERTA ENVIADA A:\n$contactName", // Muestra nombre del contacto
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    viewModel.setAlarmTriggered(false)
                    onCancelAlarm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("CANCELAR", color = Color.White)
            }
        } else {
            // Botón de Pánico
            Button(
                onClick = {
                    viewModel.setAlarmTriggered(true)
                    onTriggerPanic(emergencyNumber)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                modifier = Modifier.size(180.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = Color.White)
                    Text("PÁNICO", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}