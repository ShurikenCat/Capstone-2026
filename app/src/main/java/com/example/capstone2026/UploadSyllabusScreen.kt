package com.example.capstone2026

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.capstone2026.network.ApiClient
import com.example.capstone2026.network.EventDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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

            // Optional: persist read permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            scope.launch {
                loading = true
                status = "Uploading syllabus…"
                events = emptyList()

                try {
                    val pdfBytes = readBytes(context, uri)
                    val body = pdfBytes.toRequestBody("application/pdf".toMediaType())
                    val part = MultipartBody.Part.createFormData("file", "syllabus.pdf", body)

                    val response = ApiClient.api.extractSyllabus(part)
                    events = response.events
                    status = "✅ Extracted ${response.count} events"
                } catch (e: Exception) {
                    status = "❌ Error: ${e.message}"
                } finally {
                    loading = false
                }
            }
        }
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Upload Syllabus", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(status)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { picker.launch(arrayOf("application/pdf")) },
            enabled = !loading
        ) {
            Text(if (loading) "Working…" else "Pick PDF")
        }

        Spacer(Modifier.height(16.dp))

        if (events.isNotEmpty()) {
            Text("Extracted Events", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            events.take(40).forEach { e ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${e.type}: ${e.description.orEmpty()}")
                        Text("Date: ${e.date}", style = MaterialTheme.typography.bodySmall)
                        if (!e.assignment.isNullOrBlank()) {
                            Text("Assignment: ${e.assignment}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun readBytes(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not open PDF input stream")
    }