package com.example.capstone2026.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone2026.data.Course
import com.example.capstone2026.repository.ScheduleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScheduleRepository(application)

    val courses: StateFlow<List<Course>> =
        repository.getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultView: StateFlow<String> =
        repository.defaultView
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "week")

    fun addSampleCourse() {
        viewModelScope.launch {
            repository.addSampleCourse()
        }
    }

    fun setDefaultView(view: String) {
        viewModelScope.launch {
            repository.setDefaultView(view)
        }
    }

}