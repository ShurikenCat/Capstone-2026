package com.example.capstone2026.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone2026.data.CalendarEvent
import com.example.capstone2026.util.formatDate
import com.example.capstone2026.util.formatEventDate

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
        Box {
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
                        text = "Event Type: ${event.eventType}",
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
                        text = "Notes: ${event.notes}",
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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