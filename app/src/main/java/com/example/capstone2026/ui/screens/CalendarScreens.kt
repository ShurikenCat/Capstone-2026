package com.example.capstone2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.capstone2026.data.CalendarEvent
import com.example.capstone2026.util.*
import com.example.capstone2026.ui.components.AddJsonEvent
import com.example.capstone2026.ui.components.EditEventDialog
import com.example.capstone2026.util.saveEventsToIcs
import com.example.capstone2026.ui.components.AppMenu
import com.example.capstone2026.ui.components.EventCard
import com.example.capstone2026.ui.components.ScheduleModeSwitch
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext

/**
 * Displays all events for a single selected day.
 * Allows navigation between days and editing/deleting events.
 */
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

/**
 * Displays a weekly calendar view with indicators for days that contain events.
 * Allows navigation to daily view by selecting a date.
 */
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

/**
 * Displays a full monthly calendar view.
 * Shows event indicators and allows selecting a date to view details.
 */
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