package com.example.capstone2026

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*



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
                text = if (isExpanded) "×" else "+",
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

    // Load events from ICS
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

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {

        composable("home") {
            HomeScreen(navController)
        }
        composable("upload"){
            UploadSyllabusScreen()
        }

        // this is the default schedule
        composable("schedule") {
            WeeklyScheduleScreen(navController, allEvents)
        }

        composable("schedule_daily") {
            DailyScheduleScreen(navController, allEvents)
        }

        composable("schedule_daily/{date}") { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getString("date")
            DailyScheduleScreen(navController, allEvents, dateArg)
        }

        composable("schedule_weekly") {
            WeeklyScheduleScreen(navController, allEvents)
        }

        composable("schedule_monthly") {
            MonthlyScheduleScreen(navController, allEvents)
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
    }
}

//@Composable
//fun ScheduleScreen(navController: NavController) {
//
//    val context = LocalContext.current
//
//    // Load events once
//    val events by remember {
//        mutableStateOf(
//            try {
//                val input = loadIcsFromAssets(context, "schedule.ics")
//                parseIcsFile(input)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                emptyList()
//            }
//        )
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            if (events.isEmpty()) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .weight(1f),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("No events found")
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .weight(1f)
//                ) {
//                    items(events) { event: CalendarEvent ->
//                        EventCard(event)
//                    }
//                }
//            }
//        }
//
//        Box(
//            modifier = Modifier.align(Alignment.BottomEnd)
//        ) {
//            AppMenu(navController)
//        }
//    }
//}

enum class EventMode { SINGLE_DAY, REPEATING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (List<CalendarEvent>) -> Unit
) {
    var title by remember { mutableStateOf("") }

    var startTimeRaw by remember { mutableStateOf("") }
    var startAmPm by remember { mutableStateOf("AM") }

    var endTimeRaw by remember { mutableStateOf("") }
    var endAmPm by remember { mutableStateOf("AM") }

    val amPmOptions = listOf("AM", "PM")

    var mode by remember { mutableStateOf(EventMode.SINGLE_DAY) }
    val allDays = remember { java.time.DayOfWeek.values().toList() }
    var repeatDayOfWeek by remember { mutableStateOf(selectedDate.dayOfWeek) }
    var repeatEndDate by remember { mutableStateOf(selectedDate.plusWeeks(4)) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    fun clampMinutes(minutes: Int): Int = minutes.coerceIn(0, 59)

    fun clampHour12(h: Int): Int {
        if (h <= 0) return 12
        if (h > 12) return 12
        return h
    }

    fun formatWithColon(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(4)
        if (digits.isEmpty()) return ""
        val padded = digits.padStart(4, '0')
        val hRaw = padded.substring(0, 2).toInt()
        val h = clampHour12(hRaw)
        val mRaw = padded.substring(2, 4).toInt()
        val m = clampMinutes(mRaw)
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun parseTime12h(raw: String, amPm: String): java.time.LocalTime {
        val digits = raw.filter { it.isDigit() }
        require(digits.length in 3..4)
        val padded = digits.padStart(4, '0')
        val h12Raw = padded.substring(0, 2).toInt()
        val h12 = clampHour12(h12Raw)
        val mRaw = padded.substring(2, 4).toInt()
        val m = clampMinutes(mRaw)
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
        title = { Text("Add Event for $selectedDate") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mode:")
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == EventMode.SINGLE_DAY,
                        onClick = { mode = EventMode.SINGLE_DAY },
                        label = { Text("Single day") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == EventMode.REPEATING,
                        onClick = { mode = EventMode.REPEATING },
                        label = { Text("Weekly repeat") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("Start time")
                Row {
                    var startFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = if (startFocused) startTimeRaw else formatWithColon(startTimeRaw),
                        onValueChange = { input ->
                            startTimeRaw = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("HHMM") },
                        modifier = Modifier
                            .weight(2f)
                            .onFocusChanged { state ->
                                startFocused = state.isFocused
                            }
                    )

                    Spacer(Modifier.width(8.dp))

                    var startExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { startExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(startAmPm)
                        }
                        DropdownMenu(
                            expanded = startExpanded,
                            onDismissRequest = { startExpanded = false }
                        ) {
                            amPmOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        startAmPm = option
                                        startExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("End time (optional)")
                Row {
                    var endFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = if (endFocused) endTimeRaw else formatWithColon(endTimeRaw),
                        onValueChange = { input ->
                            endTimeRaw = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("HHMM") },
                        modifier = Modifier
                            .weight(2f)
                            .onFocusChanged { state ->
                                endFocused = state.isFocused
                            }
                    )

                    Spacer(Modifier.width(8.dp))

                    var endExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { endExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(endAmPm)
                        }
                        DropdownMenu(
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

                if (mode == EventMode.REPEATING) {
                    Spacer(Modifier.height(16.dp))
                    Text("Repeats every week on")

                    var dayExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { dayExpanded = true }) {
                            Text(
                                repeatDayOfWeek.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }
                            )
                        }
                        DropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            allDays.forEach { dow ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            dow.name.lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    },
                                    onClick = {
                                        repeatDayOfWeek = dow
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Repeat until")

                    OutlinedButton(onClick = { showEndDatePicker = true }) {
                        Text(repeatEndDate.toString())
                    }

                    if (showEndDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = repeatEndDate
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        )

                        DatePickerDialog(
                            onDismissRequest = { showEndDatePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val millis = datePickerState.selectedDateMillis
                                        if (millis != null) {
                                            val localDate = Instant.ofEpochMilli(millis)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                            repeatEndDate = localDate
                                        }
                                        showEndDatePicker = false
                                    }
                                ) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEndDatePicker = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    if (title.isBlank()) {
                        onDismiss()
                        return@TextButton
                    }

                    val zoneId = ZoneId.systemDefault()

                    val startLocalTime = parseTime12h(startTimeRaw, startAmPm)
                    val endLocalTime =
                        if (endTimeRaw.filter { it.isDigit() }.isNotBlank())
                            parseTime12h(endTimeRaw, endAmPm)
                        else null

                    // Helper to build one CalendarEvent for a given LocalDate
                    fun buildEventForDate(date: LocalDate): CalendarEvent {
                        val startInstant = date.atTime(startLocalTime)
                            .atZone(zoneId)
                            .toInstant()
                        val startDate = Date.from(startInstant)

                        val endDate = endLocalTime?.let { lt ->
                            val endInstant = date.atTime(lt)
                                .atZone(zoneId)
                                .toInstant()
                            Date.from(endInstant)
                        }

                        return CalendarEvent(
                            title = title,
                            start = startDate,
                            end = endDate
                        )
                    }

                    val events: List<CalendarEvent> =
                        if (mode == EventMode.SINGLE_DAY) {
                            listOf(buildEventForDate(selectedDate))
                        } else {
                            val results = mutableListOf<CalendarEvent>()

                            var current = selectedDate
                            while (current.dayOfWeek != repeatDayOfWeek) {
                                current = current.plusDays(1)
                            }

                            while (!current.isAfter(repeatEndDate)) {
                                results.add(buildEventForDate(current))
                                current = current.plusWeeks(1)
                            }

                            results
                        }

                    onSave(events)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
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
fun DailyScheduleScreen(
    navController: NavController,
    allEvents: SnapshotStateList<CalendarEvent>,
    dateArg: String? = null
) {
    val context = LocalContext.current

    val initialDate = remember(dateArg) {
        dateArg?.let { LocalDate.parse(it) } ?: LocalDate.now()
    }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                TextButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                    Text("<")
                }

                Text(
                    text = selectedDate.toString(),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
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
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                ) {
                    Text("+")
                }

                AppMenu(navController)
            }
        }

        if (showAddDialog) {
            AddEventDialog(
                selectedDate = selectedDate,
                onDismiss = { showAddDialog = false },
                onSave = { events ->
                    allEvents.addAll(events)
                    saveEventsToIcs(context, allEvents)
                    val file = ensureWritableIcs(context)
                    println("ICS content:\n" + file.readText())
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun WeeklyScheduleScreen(
    navController: NavController,
    allEvents: List<CalendarEvent>
) {
    val eventsByDate = allEvents.groupBy { it.start.toLocalDate() }

    val today = remember { LocalDate.now() }

    val firstDayOfWeek = java.time.DayOfWeek.SUNDAY
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }

    val state = rememberWeekCalendarState(
        startDate = today.minusWeeks(52),
        endDate = today.plusWeeks(52),
        firstVisibleWeekDate = today,
        firstDayOfWeek = firstDayOfWeek
    )

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
                text = "Week of $weekStart",
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
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun MonthlyScheduleScreen(
    navController: NavController,
    allEvents: List<CalendarEvent>
) {
    val eventsByDate = allEvents.groupBy { it.start.toLocalDate() }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }

    val firstDayOfWeek = java.time.DayOfWeek.SUNDAY
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek) }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ScheduleModeSwitch(
                navController = navController,
                currentRoute = "schedule_monthly"
            )

            Spacer(Modifier.height(8.dp))

            val visibleMonth = state.firstVisibleMonth.yearMonth
            Text(
                text = visibleMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } +
                        " ${visibleMonth.year}",
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

            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    val date = day.date
                    val hasEvents = eventsByDate[date]?.isNotEmpty() == true
                    val isToday = date == LocalDate.now()
                    val isCurrentMonth = date.month == visibleMonth.month

                    Surface(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .clickable {
                                navController.navigate("schedule_daily/$date")
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

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "Home Screen",
            modifier = Modifier.align(Alignment.Center)
        )

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
    onThemeModeChange: (ThemeMode) -> Unit
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
fun EventCard(
    event: CalendarEvent,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = event.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Start: ${formatDate(event.start)}",
                fontSize = 14.sp
            )

            event.end?.let {
                Text(
                    text = "End: ${formatDate(it)}",
                    fontSize = 14.sp
                )
            }

            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDelete,
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

fun formatDate(date: Date): String {

    val formatter =
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    return formatter.format(date)
}

//@Preview(showBackground = true)
//@Composable
//fun ScheduleScreenPreview(){
//    Capstone2026Theme {
//        AppNavGraph(
//            modifier = Modifier.padding(innerPadding),
//            themeMode = themeMode,
//            onThemeModeChange = { themeMode = it }
//        )
//    }
//}

// helper functions
data class CalendarEvent(
    val title: String,
    val start: Date,
    val end: Date? = null,
    val repeatRule: RepeatRule? = null
)

data class RepeatRule(
    val frequency: RepeatFrequency,
    val dayOfWeek: java.time.DayOfWeek,
    val until: LocalDate
)

enum class RepeatFrequency { WEEKLY }

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

    for ((index, event) in this.withIndex()) {
        val cleanTitle = event.title
            .replace("\r", " ")
            .replace("\n", " ")

        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:app-$index@capstone2026")
        sb.appendLine("SUMMARY:$cleanTitle")
        sb.appendLine("DTSTART:${fmt.format(event.start)}")
        event.end?.let {
            sb.appendLine("DTEND:${fmt.format(it)}")
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