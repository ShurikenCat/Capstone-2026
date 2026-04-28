package com.example.capstone2026.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.capstone2026.data.CalendarEvent
import com.example.capstone2026.util.saveEventsToIcs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
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


    Box {
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

/**
 * Floating action button and dialog for creating new events.
 * Saves events to JSON and updates the ICS file.
 */
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

/**
 * Dialog for editing an existing event.
 * Updates event data and persists changes.
 */
@Composable
fun EditEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var eventType by remember { mutableStateOf(event.eventType ?: "") }
    var notes by remember { mutableStateOf(event.notes ?: "") }
    var isAllDay by remember { mutableStateOf(event.isAllDay) }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                    Text("All day event")
                }
                if(!isAllDay) {
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

                        val updatedEvent = if (isAllDay) {
                            val startDateTime = date.atStartOfDay()

                            CalendarEvent(
                                title = title,
                                start = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                                end = null,
                                eventType = eventType.ifBlank { null },
                                notes = notes.ifBlank { null },
                                isAllDay = true,
                                courseTitle = event.courseTitle
                            )
                        } else {
                            val startDateTime = date.atTime(startHour.toInt(), startMinute.toInt())
                            val endDateTime = date.atTime(endHour.toInt(), endMinute.toInt())

                            if (!endDateTime.isAfter(startDateTime)) {
                                timeError = "End time must be later than start time"
                                return@TextButton
                            } else {
                                timeError = null
                            }

                            CalendarEvent(
                                title = title,
                                start = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                                end = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                                eventType = eventType.ifBlank { null },
                                notes = notes.ifBlank { null },
                                isAllDay = false,
                                courseTitle = event.courseTitle
                            )
                        }

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