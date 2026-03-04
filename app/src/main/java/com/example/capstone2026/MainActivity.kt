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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone2026.ui.theme.Capstone2026Theme
import com.example.capstone2026.ui.theme.ThemeMode
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

private val navigation: Any
    get() {
        TODO()
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }

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
                        onThemeModeChange = { themeMode = it }
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

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {

        composable("home") {
            HomeScreen(navController)
        }

        // this is the default schedule
        composable("schedule") {
            WeeklyScheduleScreen(navController)
        }

        composable("schedule_daily") {
            DailyScheduleScreen(navController)
        }

        composable("schedule_daily/{date}") { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getString("date")
            DailyScheduleScreen(navController, dateArg)
        }

        composable("schedule_weekly") {
            WeeklyScheduleScreen(navController)
        }

        composable("schedule_monthly") {
            MonthlyScheduleScreen(navController)
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

@Composable
fun DailyScheduleScreen(
    navController: NavController,
    dateArg: String? = null
) {
    val context = LocalContext.current

    val allEvents by remember {
        mutableStateOf(
            try {
                val input = loadIcsFromAssets(context, "schedule.ics")
                parseIcsFile(input)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        )
    }

    // If dateArg is provided, parse it; otherwise use today
    val initialDate = remember(dateArg) {
        dateArg?.let { LocalDate.parse(it) } ?: LocalDate.now()
    }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val eventsForDay = remember(selectedDate, allEvents) {
        allEvents
            .filter { event -> event.start.toLocalDate() == selectedDate }
            .sortedBy { it.start }
    }

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

            // Date header + previous/next day buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                    Text("<")
                }

                Text(
                    text = selectedDate.toString(), // or format nicer
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
                        EventCard(event)
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun WeeklyScheduleScreen(navController: NavController) {
    val context = LocalContext.current

    val events by remember {
        mutableStateOf(
            try {
                val input = loadIcsFromAssets(context, "schedule.ics")
                parseIcsFile(input)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        )
    }

    val eventsByDate = remember(events) {
        events.groupBy { it.start.toLocalDate() }
    }

    val today = remember { LocalDate.now() }

    // Choose the first day of week – here Sunday
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
fun MonthlyScheduleScreen(navController: NavController) {
    val context = LocalContext.current

    val events by remember {
        mutableStateOf(
            try {
                val input = loadIcsFromAssets(context, "schedule.ics")
                parseIcsFile(input)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        )
    }

    val eventsByDate = remember(events) {
        events.groupBy { it.start.toLocalDate() }
    }

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
fun EventCard(event: CalendarEvent) {

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
    val end: Date?
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