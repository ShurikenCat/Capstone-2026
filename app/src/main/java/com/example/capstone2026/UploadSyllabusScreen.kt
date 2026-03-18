package com.example.capstone2026

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.capstone2026.network.ApiClient
import com.example.capstone2026.network.EventDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun UploadSyllabusScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var status by remember { mutableStateOf("Pick a PDF syllabus to extract events.") }
    var loading by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<EventDto>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            scope.launch {
                loading = true
                status = "Uploading syllabus..."
                events = emptyList()

                try {
                    val pdfBytes = readBytes(context, uri)
                    val body = pdfBytes.toRequestBody("application/pdf".toMediaType())
                    val part = MultipartBody.Part.createFormData("file", "syllabus.pdf", body)

                    val response = ApiClient.api.extractSyllabus(part)
                    events = response.events.sortedBy { it.date }
                    status = if (response.count > 0) {
                        "✅ Extracted ${response.count} events"
                    } else {
                        "⚠️ No events found"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    status = "❌ Error: ${e.message}"
                } finally {
                    loading = false
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Upload Syllabus", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(status)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { picker.launch(arrayOf("application/pdf")) },
            enabled = !loading
        ) {
            Text(if (loading) "Working..." else "Pick PDF")
        }

        Spacer(Modifier.height(16.dp))

        if (events.isNotEmpty()) {
            Text("Extracted Events", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(events.take(40)) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

@Composable
fun EventCard(event: EventDto) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "${event.type}: ${event.description.orEmpty()}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Date: ${event.date}",
                style = MaterialTheme.typography.bodySmall
            )

            if (!event.assignment.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Assignment: ${event.assignment}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (event.date.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { addEventToCalendar(context, event) }
                ) {
                    Text("Add to Calendar")
                }
            }
        }
    }
}

fun addEventToCalendar(context: Context, event: EventDto) {
    if (event.date.isBlank()) return

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val parsedDate = formatter.parse(event.date) ?: return

    val beginCalendar = Calendar.getInstance().apply {
        time = parsedDate
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    val endCalendar = Calendar.getInstance().apply {
        time = parsedDate
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    val title = when (event.type.lowercase()) {
        "exam" -> "Exam: ${event.description.orEmpty()}"
        "quiz" -> "Quiz: ${event.description.orEmpty()}"
        "assignment" -> "Assignment: ${event.description.orEmpty()}"
        "project" -> "Project: ${event.description.orEmpty()}"
        else -> event.description.orEmpty().ifBlank { "Course Event" }
    }

    val details = buildString {
        append("Type: ${event.type}")
        if (!event.description.isNullOrBlank()) {
            append("\nDescription: ${event.description}")
        }
        if (!event.assignment.isNullOrBlank()) {
            append("\nAssignment: ${event.assignment}")
        }
    }

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, details)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginCalendar.timeInMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCalendar.timeInMillis)
    }

    context.startActivity(intent)
}

private suspend fun readBytes(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not open PDF input stream")
    }