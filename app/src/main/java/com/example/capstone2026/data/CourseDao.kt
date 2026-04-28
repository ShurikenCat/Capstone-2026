package com.example.capstone2026.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM Course ORDER BY dayOfWeek, startTime")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertCourse(course: Course)

    @Query("DELETE FROM Course")
    suspend fun deleteAll()
}