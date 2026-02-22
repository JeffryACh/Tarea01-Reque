package com.example.tarea01_reque.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safewalk_preferences")

class DataStoreManager(private val context: Context) {
    companion object {
        val EMERGENCY_NUMBER_KEY = stringPreferencesKey("emergency_number")
        val CONTACT_NAME_KEY = stringPreferencesKey("contact_name") // Nuevo para cumplir PDF
    }

    suspend fun saveContactInfo(number: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[EMERGENCY_NUMBER_KEY] = number
            preferences[CONTACT_NAME_KEY] = name
        }
    }

    val emergencyNumberFlow: Flow<String> = context.dataStore.data.map { it[EMERGENCY_NUMBER_KEY] ?: "" }
    val contactNameFlow: Flow<String> = context.dataStore.data.map { it[CONTACT_NAME_KEY] ?: "Contacto" }
}