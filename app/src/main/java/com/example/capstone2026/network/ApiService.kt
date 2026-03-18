package com.example.capstone2026.network
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ExtractResponse(
    val count: Int = 0,
    val events: List<EventDto> = emptyList()
)

data class EventDto(
    val date: String = "",
    val type: String = "",
    val description: String? = null,
    val assignment: String? = null
)

interface ApiService {
    @Multipart
    @POST("extract")
    suspend fun extractSyllabus(
        @Part file: MultipartBody.Part
    ): ExtractResponse
}