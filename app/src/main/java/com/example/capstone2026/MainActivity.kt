package com.example.capstone2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.capstone2026.ui.theme.Capstone2026Theme
import com.example.capstone2026.ui.theme.ThemeMode
import com.example.capstone2026.ui.theme.readThemeMode
import com.example.capstone2026.ui.theme.saveThemeMode
import kotlinx.coroutines.launch
import com.example.capstone2026.navigation.AppNavGraph

/**
 * Entry point of the Android application.
 * Initializes theme settings and launches the main navigation graph.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeModeFlow = applicationContext.readThemeMode()
            val themeMode by themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            Capstone2026Theme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        modifier = Modifier.padding(innerPadding),
                        themeMode = themeMode,
                        onThemeModeChange = { newMode ->
                            lifecycleScope.launch {
                                applicationContext.saveThemeMode(newMode)
                            }
                        }
                    )
                }
            }
        }
    }
}