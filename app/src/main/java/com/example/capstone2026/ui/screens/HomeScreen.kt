package com.example.capstone2026.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone2026.R
import com.example.capstone2026.data.CalendarEvent
import com.example.capstone2026.ui.components.AddJsonEvent
import com.example.capstone2026.ui.components.AppMenu
import com.example.capstone2026.util.cleanedEventTitle
import com.example.capstone2026.util.formatEventDate
import com.example.capstone2026.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Main dashboard screen.
 * Shows current date, upcoming events, and navigation to other views.
 */
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val eventDate = event.start.toLocalDate()
                                navController.navigate("schedule_daily/$eventDate")
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = cleanedEventTitle(event.title),
                            fontWeight = FontWeight.Bold)
                            event.courseTitle?.let {
                                Text(
                                    text = it,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = formatEventDate(event)
                            ,
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