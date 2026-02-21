package com.example.tarea01_reque.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creamos una extensión del Context para asegurar que el DataStore sea un Singleton en toda la app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safewalk_preferences")

class DataStoreManager(private val context: Context) {

    // Definimos las "llaves" con las que guardaremos y buscaremos nuestros datos
    companion object {
        val EMERGENCY_NUMBER_KEY = stringPreferencesKey("emergency_number")
    }

    /**
     * Guarda el número de teléfono en el almacenamiento físico del dispositivo.
     * Al ser una operación de entrada/salida (I/O), debe ser una función 'suspend'
     * para ejecutarse dentro de una corrutina y no bloquear la interfaz.
     */
    suspend fun saveEmergencyNumber(number: String) {
        context.dataStore.edit { preferences ->
            preferences[EMERGENCY_NUMBER_KEY] = number
        }
    }

    /**
     * Un flujo (Flow) constante que emite el número de emergencia.
     * Si no hay ningún número guardado aún, devuelve un String vacío ("").
     */
    val emergencyNumberFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[EMERGENCY_NUMBER_KEY] ?: ""
        }
}