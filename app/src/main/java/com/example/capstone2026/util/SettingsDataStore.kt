package com.example.capstone2026.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property on Context
val Context.settingsDataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DEFAULT_VIEW = stringPreferencesKey("default_view") // "day" or "week"
    val NOTIFY_MIN_BEFORE = intPreferencesKey("notify_min_before")
}

class SettingsRepository(private val context: Context) {

    private val dataStore = context.settingsDataStore

    val defaultView: Flow<String> =
        dataStore.data.map { prefs ->
            prefs[SettingsKeys.DEFAULT_VIEW] ?: "week"
        }

    suspend fun setDefaultView(view: String) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.DEFAULT_VIEW] = view
        }
    }

    val notifyMinutesBefore: Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[SettingsKeys.NOTIFY_MIN_BEFORE] ?: 10
        }

    suspend fun setNotifyMinutesBefore(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.NOTIFY_MIN_BEFORE] = minutes
        }
    }
}