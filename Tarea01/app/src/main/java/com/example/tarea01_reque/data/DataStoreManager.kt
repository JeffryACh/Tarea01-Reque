// DataStoreManager.kt
package com.example.tarea01_reque.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "safe_walk_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        private val PHONE_KEY = stringPreferencesKey("emergency_phone")
    }

    // Guardar teléfono
    suspend fun savePhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[PHONE_KEY] = phone
        }
    }

    // Obtener teléfono como Flow
    val getPhone: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PHONE_KEY] ?: ""
        }
}