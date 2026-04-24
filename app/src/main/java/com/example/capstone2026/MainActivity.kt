package com.example.capstone2026

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone2026.ui.theme.Capstone2026Theme
import com.example.capstone2026.ui.theme.ThemeMode
import com.example.capstone2026.ui.theme.readThemeMode
import com.example.capstone2026.ui.theme.saveThemeMode
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import java.time.DayOfWeek
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

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

@Composable
fun AppMenu(
    navController: NavController
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End
    ) {

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Home") {
                navController.navigate("home") {
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Upload") {
                navController.navigate("upload"){
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Schedule") {
                navController.navigate("schedule") {
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Settings") {
                navController.navigate("settings") {
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isExpanded) "×" else "=",
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val initialEvents by remember {
        mutableStateOf(
            try {
                val file = ensureWritableIcs(context)
                val input = file.inputStream()
                parseIcsFile(input)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
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
                onImportEvents = { extractedEvents ->
                    val converted = extractedEvents.mapNotNull { it.toCalendarEvent() }

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
@Composable
fun AddJsonEvent(
    allEvents: SnapshotStateList<CalendarEvent>,
    dateArg: String? = null,
    ) {
    val context = LocalContext.current

    val initialDate = remember(dateArg) {
        dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    }

    var selectedDate by remember { mutableStateOf(initialDate) }

    var showAddDialog by remember { mutableStateOf(false) }


    Box() {
        Box(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Event")
                }
            }
        }
        fun saveJsonToFile(context: Context, event: CalendarEventJson){
            val fileName = "events.json"
            val file = File(context.filesDir, fileName)
            val jsonString = Json {
                prettyPrint = true
                ignoreUnknownKeys
            }
            val currentEvents: MutableList<CalendarEventJson> =
                if(file.exists()) {
                    try{
                        jsonString.decodeFromString<MutableList<CalendarEventJson>>(file.readText())
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                } else {
                    mutableListOf()
                }
            currentEvents.add(event)
            val updatedJson = jsonString.encodeToString(currentEvents)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(updatedJson.toByteArray())
            }
        }
        if (showAddDialog) {
            AddEventJson(
                selectedDate = selectedDate,
                onDismiss = { showAddDialog = false },
                onSave = { event, event2 ->
                    saveJsonToFile(
                        context = context,
                        event = event
                    )
                    allEvents.addAll(event2)
                    saveEventsToIcs(context, allEvents)
                    showAddDialog = false
                }
            )
        }


    }
}


@Serializable
data class CalendarEventJson(
    val title: String,
    val eventType: String,
    val inflexible: Boolean,
    val notes: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val repeated: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventJson(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (CalendarEventJson, List<CalendarEvent>) -> Unit
) {
    var title by remember { mutableStateOf("") }

    var eventType by remember { mutableStateOf("Class") }
    var inflexible by remember { mutableStateOf("True") }
    var notes by remember { mutableStateOf("") }
    var startDateText by remember { mutableStateOf(selectedDate.toString()) }
    var timeError by remember { mutableStateOf<String?>(null) }
    val parsedStartDate = try {
        LocalDate.parse(startDateText)
    } catch (e: Exception) {
        null
    }
    var endDateText by remember { mutableStateOf(selectedDate.toString()) }
    val parsedEndDate = try {
        LocalDate.parse(endDateText)
    } catch (e: Exception) {
        null
    }

    var sun by remember { mutableStateOf(false) }
    val sunText = if(sun) {
        "Sun, "
    } else {
        ""
    }
    var mon by remember { mutableStateOf(false) }
    val monText = if(mon) {
        "Mon, "
    } else {
        ""
    }
    var tue by remember { mutableStateOf(false) }
    val tueText = if(tue) {
        "Tue, "
    } else {
        ""
    }
    var wed by remember { mutableStateOf(false) }
    val wedText = if(wed) {
        "Wed, "
    } else {
        ""
    }
    var thu by remember { mutableStateOf(false) }
    val thuText = if(thu) {
        "Thu, "
    } else {
        ""
    }
    var fri by remember { mutableStateOf(false) }
    val friText = if(fri) {
        "Fri, "
    } else {
        ""
    }
    var sat by remember { mutableStateOf(false) }
    val satText = if(sat) {
        "Sat, "
    } else {
        ""
    }



    // digit input for start/end
    var startHr by remember { mutableStateOf("09") }
    var startMin by remember { mutableStateOf("00") }
    var startAmPm by remember { mutableStateOf("AM") }

    var endHr by remember { mutableStateOf("05") }
    var endMin by remember { mutableStateOf("00") }
    var endAmPm by remember { mutableStateOf("PM") }

    val inflexibleOptions = listOf("True", "False")
    val eventOptions = listOf("Class", "Office Hours", "Meeting", "Sleep", "Other")
    val amPmOptions = listOf("AM", "PM")
    val hourOptions = (1..12).map { it.toString().padStart(2, '0') }
    val minuteOptions = (0..59).map { it.toString().padStart(2, '0') }



    fun clampMinutes(minutes: Int): Int = minutes.coerceIn(0, 59)

    // Clamp hours to 1–12 (0 or >12 → 12)
    fun clampHour12(h: Int): Int {
        if (h <= 0) return 12
        if (h > 12) return 12
        return h
    }


    // Parse raw digits + AM/PM into LocalTime
    fun parseTime12h(hrStr: String, minStr: String, amPm: String): java.time.LocalTime {
        val h12 = hrStr.toInt()
        val m = minStr.toInt()
        val h24 = when {
            h12 == 12 && amPm == "AM" -> 0
            h12 == 12 && amPm == "PM" -> 12
            amPm == "PM" -> h12 + 12
            else -> h12
        }
        return java.time.LocalTime.of(h24, m)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        startDateText = it
                    },
                    label = { Text("Start Date (yyyy-mm-dd)") },
                    isError = parsedStartDate == null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = endDateText,
                    onValueChange = {
                        endDateText = it
                    },
                    label = { Text("End Date (yyyy-mm-dd)") },
                    isError = parsedEndDate == null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("Repeated?")
                Text("(Sun,    Mon,   Tue,    Wed,   Thu,    Fri,    Sat)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = sun,
                        onCheckedChange = { sun = it }
                    )
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = mon,
                        onCheckedChange = { mon = it }
                    )
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = tue,
                        onCheckedChange = { tue = it }
                    )
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = wed,
                        onCheckedChange = { wed = it }
                    )
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = thu,
                        onCheckedChange = { thu = it }
                    )
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = fri,
                        onCheckedChange = { fri = it }
                    )
                    Checkbox(
                        modifier = Modifier.width((40.dp)),
                        checked = sat,
                        onCheckedChange = { sat = it }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("Event Type                    Is the event fixed?")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var eventTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = eventTypeExpanded,
                        onExpandedChange = {eventTypeExpanded = !eventTypeExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = eventType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventTypeExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = eventTypeExpanded,
                            onDismissRequest = { eventTypeExpanded = false }
                        ) {
                            eventOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        eventType = option
                                        eventTypeExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    var inflexibleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = inflexibleExpanded,
                        onExpandedChange = {inflexibleExpanded = !inflexibleExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = inflexible,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = inflexibleExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = inflexibleExpanded,
                            onDismissRequest = { inflexibleExpanded = false }
                        ) {
                            inflexibleOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        inflexible = option
                                        inflexibleExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(12.dp))

                Text("Start Time")
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var hourExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = hourExpanded,
                        onExpandedChange = {hourExpanded = !hourExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = startHr,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = hourExpanded,
                            onDismissRequest = { hourExpanded = false },
                        ) {
                            hourOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        startHr = option
                                        hourExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    var minuteExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = minuteExpanded,
                        onExpandedChange = {minuteExpanded = !minuteExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = startMin,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = minuteExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = minuteExpanded,
                            onDismissRequest = { minuteExpanded = false }
                        ) {
                            minuteOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        startMin = option
                                        minuteExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    var startExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox (
                        expanded = startExpanded,
                        onExpandedChange = {startExpanded = !startExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = startAmPm,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = startExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = startExpanded,
                            onDismissRequest = { startExpanded = false }
                        ) {
                            amPmOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        startAmPm = option
                                        startExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("End Time")
                Row(modifier = Modifier.fillMaxWidth()) {
                    var hourExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox (
                        expanded = hourExpanded,
                        onExpandedChange = {hourExpanded = !hourExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = endHr,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = hourExpanded,
                            onDismissRequest = { hourExpanded = false }
                        ) {
                            hourOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        endHr = option
                                        hourExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var minuteExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox (
                        expanded = minuteExpanded,
                        onExpandedChange = {minuteExpanded = !minuteExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = endMin,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = minuteExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = minuteExpanded,
                            onDismissRequest = { minuteExpanded = false },
                        ) {
                            minuteOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        endMin = option
                                        minuteExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var endExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox (
                        expanded = endExpanded,
                        onExpandedChange = {endExpanded = !endExpanded},
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = endAmPm,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = endExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = endExpanded,
                            onDismissRequest = { endExpanded = false }
                        ) {
                            amPmOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        endAmPm = option
                                        endExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                timeError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.width(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    if (title.isBlank()) {
                        onDismiss()
                        return@TextButton
                    }
                    if (parsedStartDate == null) {
                        onDismiss()
                        return@TextButton
                    }
                    if (parsedEndDate == null) {
                        onDismiss()
                        return@TextButton
                    }

                    val startLocalTime = parseTime12h(startHr, startMin, startAmPm)
                    val endLocalTime = parseTime12h(endHr, endMin, endAmPm)

                    if (!endLocalTime.isAfter(startLocalTime)) {
                        timeError = "End time must be later than start time"
                        return@TextButton
                    } else {
                        timeError = null
                    }

                    val repeated = sunText + monText + tueText + wedText + thuText + friText + satText

                    val jsonEvent = CalendarEventJson(
                        title = title,
                        eventType = eventType,
                        inflexible = inflexible == "True",
                        notes = notes,
                        startDate = startDateText,
                        endDate = endDateText,
                        startTime = "$startHr:$startMin $startAmPm",
                        endTime = "$endHr:$endMin $endAmPm",
                        repeated = if (repeated == "") null else repeated
                    )

                    val zoneId = ZoneId.systemDefault()

                    fun buildEventForDate(date: LocalDate): CalendarEvent {
                        val startInstant = date.atTime(startLocalTime)
                            .atZone(zoneId)
                            .toInstant()
                        val startDate = Date.from(startInstant)

                        val endInstant = date.atTime(endLocalTime)
                            .atZone(zoneId)
                            .toInstant()
                        val endDate = Date.from(endInstant)

                        return CalendarEvent(
                            title = title,
                            start = startDate,
                            end = endDate,
                            eventType = eventType.ifBlank { null },
                            notes = notes.ifBlank { null },
                            isAllDay = false
                        )
                    }

                    val results = mutableListOf<CalendarEvent>()
                    results.add(buildEventForDate(LocalDate.parse(startDateText)))

                    if (sat) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.SATURDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }
                    if (mon) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.MONDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }
                    if (tue) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.TUESDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }
                    if (wed) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.WEDNESDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }
                    if (thu) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.THURSDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }
                    if (fri) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.FRIDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }
                    if (sun) {
                        var current = parsedStartDate.plusDays(1)
                        while (current.dayOfWeek != DayOfWeek.SUNDAY) {
                            current = current.plusDays(1)
                        }
                        while (!current.isAfter(parsedEndDate)) {
                            results.add(buildEventForDate(current))
                            current = current.plusWeeks(1)
                        }
                    }

                    onSave(jsonEvent, results)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onDismiss()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.headlineMedium
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = ""
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = passwordVisible,
                        onCheckedChange = { passwordVisible = it }
                    )
                    Text("Show password")
                }

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {

                        if (username == "admin" && password == "override") {
                            onLoginSuccess()
                        }
                        else if(username == "user" && password == "goodpassword"){
                            onLoginSuccess()
                        } else{
                            errorMessage = "Invalid username or password"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In")
                }
            }
        }
    }
}

@Composable
fun DailyScheduleScreen(
    navController: NavController,
    allEvents: SnapshotStateList<CalendarEvent>,
    initialDate: LocalDate,
    onFocusedDateChange: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(initialDate) }
    LaunchedEffect(selectedDate) {
        onFocusedDateChange(selectedDate)
    }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    val eventsForDay =
        allEvents
            .filter { event -> event.start.toLocalDate() == selectedDate }
            .sortedBy { it.start }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ScheduleModeSwitch(
                navController = navController,
                currentRoute = "schedule_daily"
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { 
                    selectedDate = selectedDate.minusDays(1)
                    onFocusedDateChange(selectedDate) 
                    }) {
                    Text("<")
                }

                Text(
                    text = formatFullLocalDate(selectedDate),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(onClick = { 
                    selectedDate = selectedDate.plusDays(1)
                    onFocusedDateChange(selectedDate)  }) {
                    Text(">")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (eventsForDay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No events this day")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(eventsForDay) { event ->
                        EventCard(
                            event = event,
                            onEdit = {
                                editingEvent = it
                            },
                            onDelete = {
                                allEvents.remove(event)
                                saveEventsToIcs(context, allEvents)
                            }
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            AddJsonEvent(allEvents)
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
    editingEvent?.let { eventToEdit ->
        EditEventDialog(
            event = eventToEdit,
            onDismiss = { editingEvent = null },
            onSave = { updatedEvent ->
                val index = allEvents.indexOf(eventToEdit)
                if (index != -1) {
                    allEvents[index] = updatedEvent
                    saveEventsToIcs(context, allEvents)
                }
                editingEvent = null
            }
        )
    }
}

@Composable
fun WeeklyScheduleScreen(
    navController: NavController,
    allEvents: SnapshotStateList<CalendarEvent>,
    focusedDate: LocalDate,
    onFocusedDateChange: (LocalDate) -> Unit
) {
    val eventsByDate = allEvents.groupBy { it.start.toLocalDate() }

    val today = focusedDate

    val firstDayOfWeek = java.time.DayOfWeek.SUNDAY
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }

    val state = rememberWeekCalendarState(
        startDate = today.minusWeeks(52),
        endDate = today.plusWeeks(52),
        firstVisibleWeekDate = today,
        firstDayOfWeek = firstDayOfWeek
    )

    LaunchedEffect(state.firstVisibleWeek.days.first().date) {
        onFocusedDateChange(state.firstVisibleWeek.days.first().date)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ScheduleModeSwitch(
                navController = navController,
                currentRoute = "schedule_weekly"
            )

            Spacer(Modifier.height(8.dp))

            val visibleWeek = state.firstVisibleWeek
            val weekStart = visibleWeek.days.first().date
            Text(
                text = "Week of ${formatWeekRange(weekStart)}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfWeek.name.take(3),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            WeekCalendar(
                state = state,
                dayContent = { day ->
                    val date = day.date
                    val hasEvents = eventsByDate[date]?.isNotEmpty() == true
                    val isToday = date == LocalDate.now()

                    Surface(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(0.8f)
                            .clickable {
                                navController.navigate("schedule_daily/$date")
                            },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isToday)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        tonalElevation = if (hasEvents) 2.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.name.take(3),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (hasEvents) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            AddJsonEvent(allEvents)
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun MonthlyScheduleScreen(
    navController: NavController,
    allEvents: SnapshotStateList<CalendarEvent>,
    focusedDate: LocalDate,
    onFocusedDateChange: (LocalDate) -> Unit
) {
    val eventsByDate = allEvents.groupBy { it.start.toLocalDate() }

    val currentMonth = remember(focusedDate) { YearMonth.from(focusedDate) }
    val startMonth = remember { currentMonth.minusYears(5) }
    val endMonth = remember { currentMonth.plusYears(5) }

    val firstDayOfWeek = java.time.DayOfWeek.SUNDAY
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val scope = rememberCoroutineScope()

    var showMonthYearPicker by remember { mutableStateOf(false) }
    val visibleMonth = state.firstVisibleMonth.yearMonth

    var selectedMonth by remember(showMonthYearPicker) { mutableStateOf(visibleMonth.monthValue) }
    var selectedYear by remember(showMonthYearPicker) { mutableStateOf(visibleMonth.year) }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val yearOptions = (startMonth.year..endMonth.year).toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ScheduleModeSwitch(
                navController = navController,
                currentRoute = "schedule_monthly"
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        selectedMonth = visibleMonth.monthValue
                        selectedYear = visibleMonth.year
                        showMonthYearPicker = true
                    }
                ) {
                    Text(
                        text = formatMonthYear(visibleMonth),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedButton(
                    onClick = {
                        val today = LocalDate.now()
                        val todayMonth = YearMonth.from(today)
                        selectedDate = today
                        onFocusedDateChange(today)

                        scope.launch {
                            state.animateScrollToMonth(todayMonth)
                        }
                    }
                ) {
                    Text("Today")
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfWeek.name.take(3),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalCalendar(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    dayContent = { day ->
                        val date = day.date
                        val hasEvents = eventsByDate[date]?.isNotEmpty() == true
                        val isToday = date == LocalDate.now()
                        val isCurrentMonth = date.month == visibleMonth.month

                        Surface(
                            modifier = Modifier
                                .padding(1.dp)
                                .aspectRatio(1f)
                                .clickable {
                                    selectedDate = date
                                    onFocusedDateChange(date)
                                },
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                !isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = if (hasEvents) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrentMonth)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.outline
                                )

                                if (hasEvents) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .align(Alignment.CenterHorizontally)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                )
            }
            selectedDate?.let { date ->

                val eventsForDay = allEvents
                    .filter { it.start.toLocalDate() == date }
                    .sortedBy { it.start }

                val selectedDateFormatted = date.format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = selectedDateFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (eventsForDay.isEmpty()) {
                            Text("No events for this day")
                        } else {
                            eventsForDay.take(3).forEach { event ->
                                Text("• ${cleanedEventTitle(event.title)}")
                                Text(
                                    text = formatDate(event.start),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (eventsForDay.size > 3) {
                                Text("+ ${eventsForDay.size - 3} more")
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        TextButton(
                            onClick = {
                                onFocusedDateChange(date)
                                navController.navigate("schedule_daily/$date")
                            }
                        ) {
                            Text("Open Daily View")
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            AddJsonEvent(allEvents)
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            monthNames = monthNames,
            yearOptions = yearOptions,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { month, year ->
                showMonthYearPicker = false
                val targetMonth = YearMonth.of(year, month)
                onFocusedDateChange(targetMonth.atDay(1))
                scope.launch {
                    state.animateScrollToMonth(targetMonth)
                }
            }
        )
    }
}

@Composable
fun MonthYearPickerDialog(
    initialMonth: Int,
    initialYear: Int,
    monthNames: List<String>,
    yearOptions: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedYear by remember { mutableStateOf(initialYear) }

    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Month and Year") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text("Month")

                Box {
                    OutlinedButton(
                        onClick = { monthExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(monthNames[selectedMonth - 1])
                    }

                    DropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        monthNames.forEachIndexed { index, monthName ->
                            DropdownMenuItem(
                                text = { Text(monthName) },
                                onClick = {
                                    selectedMonth = index + 1
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Year")

                Box {
                    OutlinedButton(
                        onClick = { yearExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedYear.toString())
                    }

                    DropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        yearOptions.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    selectedYear = year
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedMonth, selectedYear) }
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HomeScreen(
    allEvents: SnapshotStateList<CalendarEvent>,
    onNavigateToUpload: () -> Unit,
    onNavigateToDaily: () -> Unit,
    onNavigateToWeekly: () -> Unit,
    onNavigateToMonthly: () -> Unit,
    navController: NavController
) {
    val today = LocalDate.now()
    val formattedDate = today.format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d")
    )


    val upcomingEvents = allEvents
        .filter { it.start.toLocalDate() >= today }
        .sortedBy { it.start }
        .take(3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                .height(80.dp)
            )

            // Date
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.titleMedium
            )

            if (upcomingEvents.isEmpty()) {
                Text("No upcoming events")
            } else {
                upcomingEvents.forEach { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = cleanedEventTitle(event.title),
                            fontWeight = FontWeight.Bold)
                            Text(text = formatEventDate(event),
                            fontSize = 14.sp)
                            if (!event.notes.isNullOrBlank()) {
                                    Text("Notes: " + event.notes)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                Button(
                    onClick = onNavigateToUpload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload Syllabus")
                }
//                Button(onClick = onNavigateToSettings) {
//                    Text("Settings")
//                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNavigateToDaily,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Daily")
                }

                Button(
                    onClick = onNavigateToWeekly,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Weekly")
                }

                Button(
                    onClick = onNavigateToMonthly,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Monthly")
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            AddJsonEvent(allEvents)
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                    )
                    Text("Use system setting")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                    Text("Light")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                    Text("Dark")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Sign Out")
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            AppMenu(navController)
        }
    }
}

@Composable
fun SpeedDialItem(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 12.dp
            )
        )
    }
}

@Composable
fun ScheduleModeSwitch(
    navController: NavController,
    currentRoute: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextButton(
            onClick = { navController.navigate("schedule_daily") },
            enabled = currentRoute != "schedule_daily"
        ) {
            Text("Daily")
        }
        TextButton(
            onClick = { navController.navigate("schedule_weekly") },
            enabled = currentRoute != "schedule_weekly"
        ) {
            Text("Weekly")
        }
        TextButton(
            onClick = { navController.navigate("schedule_monthly") },
            enabled = currentRoute != "schedule_monthly"
        ) {
            Text("Monthly")
        }
    }
}

@Composable
fun ConfirmDelete(
    onDelete: () -> Unit,
    onDismiss: () -> Unit
    ) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delete") },
        text = { Text("Are you sure you wish to DELETE this event?") },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    )
}

@Composable
fun EditEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var eventType by remember { mutableStateOf(event.eventType ?: "") }
    var notes by remember { mutableStateOf(event.notes ?: "") }
    var timeError by remember { mutableStateOf<String?>(null) }

    val startLocal = event.start.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    val endLocal = event.end?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

    var dateText by remember { mutableStateOf(startLocal.toLocalDate().toString()) }
    var startHour by remember { mutableStateOf(startLocal.hour.toString().padStart(2, '0')) }
    var startMinute by remember { mutableStateOf(startLocal.minute.toString().padStart(2, '0')) }

    var endHour by remember {
        mutableStateOf(endLocal?.hour?.toString()?.padStart(2, '0') ?: "23")
    }
    var endMinute by remember {
        mutableStateOf(endLocal?.minute?.toString()?.padStart(2, '0') ?: "59")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { startHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Start Hr") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startMinute,
                        onValueChange = { startMinute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Start Min") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { endHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("End Hr") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endMinute,
                        onValueChange = { endMinute = it.filter(Char::isDigit).take(2) },
                        label = { Text("End Min") },
                        modifier = Modifier.weight(1f)
                    )
                }
                timeError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = eventType,
                    onValueChange = { eventType = it },
                    label = { Text("Event Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val date = LocalDate.parse(dateText)
                        val startDateTime = date.atTime(startHour.toInt(), startMinute.toInt())
                        val endDateTime = date.atTime(endHour.toInt(), endMinute.toInt())

                        if (!endDateTime.isAfter(startDateTime)) {
                            timeError = "End time must be later than start time"
                            return@TextButton
                        } else {
                            timeError = null
                        }

                        val updatedEvent = CalendarEvent(
                            title = title,
                            start = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                            end = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                            eventType = eventType.ifBlank { null },
                            notes = notes.ifBlank { null },
                            isAllDay = false
                        )

                        onSave(updatedEvent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EventCard(
    event: CalendarEvent,
    onDelete: (() -> Unit)? = null,
    onEdit: ((CalendarEvent) -> Unit)? = null
) {

    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row() {
            Box() {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = event.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                     Spacer(Modifier.height(4.dp))

                    if (!event.eventType.isNullOrBlank()) {
                        Text(
                            text = "Event Type: " + event.eventType,
                            fontSize = 14.sp
                        )
                    }
                     Text(
                         text = "Start: ${formatEventDate(event)}",
                         fontSize = 14.sp
                     )

                    event.end?.let {
                        Text(
                            text = "End: ${formatDate(it)}",
                            fontSize = 14.sp
                        )
                    }

                    if (!event.notes.isNullOrBlank()) {
                        Text(
                            text = "Notes: " + event.notes,
                            fontSize = 14.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row {
                            if (onEdit != null) {
                                TextButton(
                                    onClick = { onEdit(event) }
                                ) {
                                    Text("Edit")
                                }
                            }

                            if (onDelete != null) {
                                TextButton(
                                    onClick = { showConfirmDelete = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showConfirmDelete && onDelete != null) {
        ConfirmDelete(
            onDelete = {
                onDelete()
                showConfirmDelete = false
            },
            onDismiss = {
                showConfirmDelete = false
            }
        )
    }
}

fun formatDate(date: Date): String {

    val formatter =
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    return formatter.format(date)
}
fun formatEventDate(event: CalendarEvent): String {
    return if (event.isAllDay) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(event.start) + " • All day"
    } else {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(event.start)
    }
}
fun formatFullLocalDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
}

fun formatWeekRange(startDate: LocalDate): String {
    val endDate = startDate.plusDays(6)
    return "${startDate.format(DateTimeFormatter.ofPattern("MMM d"))} - " +
            "${endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
}

fun formatMonthYear(yearMonth: YearMonth): String {
    return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}

fun cleanedEventTitle(title: String): String {
    return title
        .replace(Regex("""\b\d{8}T\d{6}Z\b"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}


// helper functions
data class CalendarEvent(
    val title: String,
    val start: Date,
    val end: Date? = null,
    val eventType: String? = null,
    val notes: String? = null,
    val isAllDay: Boolean = false
)
fun Date.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = d1 }
    val c2 = Calendar.getInstance().apply { time = d2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

// ics file editing functions

// --- ICS file helpers ---

fun ensureWritableIcs(context: android.content.Context): File {
    val dir = context.filesDir
    val target = File(dir, "schedule.ics")

    if (!target.exists()) {
        try {
            context.assets.open("schedule.ics").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            target.writeText("BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR\n")
        }
    }
    return target
}

fun List<CalendarEvent>.toIcsString(): String {
    val sb = StringBuilder()
    sb.appendLine("BEGIN:VCALENDAR")
    sb.appendLine("VERSION:2.0")

    val fmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    fun escapeIcs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }
    for ((index, event) in this.withIndex()) {
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:app-$index@capstone2026")
        sb.appendLine("SUMMARY:${escapeIcs(event.title)}")
        sb.appendLine("DTSTART:${fmt.format(event.start)}")
        event.end?.let {
            sb.appendLine("DTEND:${fmt.format(it)}")
        }
        event.notes?.let{
            sb.appendLine("DESCRIPTION:${escapeIcs(event.notes)}")
        }
        event.eventType?.let{
            sb.appendLine("CATEGORIES:${escapeIcs(event.eventType)}")
        }
        sb.appendLine("END:VEVENT")
    }

    sb.appendLine("END:VCALENDAR")
    return sb.toString()
}

fun saveEventsToIcs(context: android.content.Context, events: List<CalendarEvent>) {
    val file = ensureWritableIcs(context)
    file.writeText(events.toIcsString())
}