package com.example.capstone2026.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone2026.ui.theme.ThemeMode
import com.example.capstone2026.data.CalendarEvent
import com.example.capstone2026.ui.screens.LoginScreen
import com.example.capstone2026.ui.screens.HomeScreen
import com.example.capstone2026.ui.screens.SettingsScreen
import java.time.LocalDate
import com.example.capstone2026.ui.screens.DailyScheduleScreen
import com.example.capstone2026.ui.screens.WeeklyScheduleScreen
import com.example.capstone2026.ui.screens.MonthlyScheduleScreen
import com.example.capstone2026.ui.screens.UploadSyllabusScreen
import com.example.capstone2026.util.ensureWritableIcs
import com.example.capstone2026.util.parseIcsFile
import com.example.capstone2026.util.saveEventsToIcs
import com.example.capstone2026.ui.screens.toCalendarEvent

/**
 * Defines all navigation routes for the app and manages shared UI state.
 * Holds global state such as events list and currently focused date.
 */
@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val initialEvents: List<CalendarEvent> by remember {
        mutableStateOf(
            try {
                val file = ensureWritableIcs(context)
                val input = file.inputStream()
                parseIcsFile(input)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList<CalendarEvent>()
            }
        )
    }

    val allEvents = remember {
        mutableStateListOf<CalendarEvent>().apply { addAll(initialEvents) }
    }

    var focusedDate by remember { mutableStateOf(LocalDate.now()) }
    var lastImportedEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                allEvents = allEvents,
                onNavigateToUpload = { navController.navigate("upload") },
                onNavigateToDaily = { navController.navigate("schedule_daily") },
                onNavigateToWeekly = { navController.navigate("schedule_weekly") },
                onNavigateToMonthly = { navController.navigate("schedule_monthly") },
                navController = navController
            )
        }
        composable("upload") {
            UploadSyllabusScreen(
                navController = navController,
                onImportEvents = { extractedEvents, courseTitle ->
                    val converted = extractedEvents.mapNotNull { it.toCalendarEvent(courseTitle) }

                    val actuallyAdded = mutableListOf<CalendarEvent>()

                    converted.forEach { newEvent ->
                        val alreadyExists = allEvents.any { existing ->
                            existing.title == newEvent.title &&
                                    existing.start.time == newEvent.start.time
                        }

                        if (!alreadyExists) {
                            allEvents.add(newEvent)
                            actuallyAdded.add(newEvent)
                        }
                    }

                    lastImportedEvents = actuallyAdded
                    saveEventsToIcs(context, allEvents)
                },
                onUndoLastImport = {
                    if (lastImportedEvents.isNotEmpty()) {
                        allEvents.removeAll(lastImportedEvents.toSet())
                        saveEventsToIcs(context, allEvents)
                        lastImportedEvents = emptyList()
                    }
                },
                canUndoLastImport = lastImportedEvents.isNotEmpty()
            )
        }

        // this is the default schedule
        composable("schedule") {
            MonthlyScheduleScreen(
                navController = navController,
                allEvents = allEvents,
                focusedDate = focusedDate,
                onFocusedDateChange = { focusedDate = it }
            )
        }

        composable("schedule_daily") {
            DailyScheduleScreen(
                navController = navController,
                allEvents = allEvents,
                initialDate = focusedDate,
                onFocusedDateChange = { focusedDate = it }
            )
        }

        composable("schedule_daily/{date}") { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getString("date")
            val parsedDate = dateArg?.let { LocalDate.parse(it) } ?: focusedDate

            DailyScheduleScreen(
                navController = navController,
                allEvents = allEvents,
                initialDate = parsedDate,
                onFocusedDateChange = { focusedDate = it }
            )
        }

        composable("schedule_weekly") {
            WeeklyScheduleScreen(
                navController = navController,
                allEvents = allEvents,
                focusedDate = focusedDate,
                onFocusedDateChange = { focusedDate = it }
            )
        }

        composable("schedule_monthly") {
            MonthlyScheduleScreen(
                navController = navController,
                allEvents = allEvents,
                focusedDate = focusedDate,
                onFocusedDateChange = { focusedDate = it }
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}