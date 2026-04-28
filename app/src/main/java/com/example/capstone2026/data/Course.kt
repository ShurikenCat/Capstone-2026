package com.example.capstone2026.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,      // "Math 101"
    val location: String?, // "Room 201"
    val dayOfWeek: Int,    // 1=Mon ... 7=Sun
    val startTime: String, // "09:00"
    val endTime: String    // "10:15"
)