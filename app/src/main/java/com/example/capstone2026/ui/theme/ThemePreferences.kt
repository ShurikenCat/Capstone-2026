package com.example.capstone2026.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

private val THEME_MODE_KEY = intPreferencesKey("theme_mode")

fun Context.readThemeMode(): Flow<ThemeMode> =
    themeDataStore.data.map { prefs ->
        val ordinal = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.ordinal
        ThemeMode.values().getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

suspend fun Context.saveThemeMode(mode: ThemeMode) {
    themeDataStore.edit { prefs ->
        prefs[THEME_MODE_KEY] = mode.ordinal
    }
}
