package com.example.capstone2026.repository

import android.content.Context
import com.example.capstone2026.util.SettingsRepository
import com.example.capstone2026.data.AppDatabase
import com.example.capstone2026.data.Course
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val courseDao = db.courseDao()
    private val settingsRepo = SettingsRepository(context)

    // Room (courses)
    fun getAllCourses(): Flow<List<Course>> = courseDao.getAllCourses()

    suspend fun addSampleCourse() {
        val sample = Course(
            name = "Sample Class",
            location = "Room 101",
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "10:00"
        )
        courseDao.insertCourse(sample)
    }


    // DataStore (settings)
    val defaultView: Flow<String> = settingsRepo.defaultView
    suspend fun setDefaultView(view: String) = settingsRepo.setDefaultView(view)
}