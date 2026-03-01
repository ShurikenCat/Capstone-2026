package com.example.capstone2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.capstone2026.ScheduleViewModel

class Temp : ComponentActivity() {

    private val viewModel: ScheduleViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val courses by viewModel.courses.collectAsState()
            val defaultView by viewModel.defaultView.collectAsState()

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Schedule App - $defaultView view") }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { viewModel.addSampleCourse() }) {
                        Text("+")
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text("Courses:")
                    Spacer(Modifier.height(8.dp))
                    courses.forEach { course ->
                        Text("${course.name} - ${course.dayOfWeek} @ ${course.startTime}")
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val next = if (defaultView == "week") "day" else "week"
                        viewModel.setDefaultView(next)
                    }) {
                        Text("Toggle view (day/week)")
                    }
                }
            }
        }
    }
}
