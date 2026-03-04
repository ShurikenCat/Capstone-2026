package com.example.capstone2026.network
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ExtractResponse(
    val count: Int,
    val events: List<EventDto>
)

data class EventDto(
    val date: String,
    val type: String,
    val description: String?,
    val assignment: String?
)

interface ApiService {
    @Multipart
    @POST("extract")
    suspend fun extractSyllabus(
        @Part file: MultipartBody.Part
    ): ExtractResponse
}