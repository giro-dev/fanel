package dev.agiro.fanel.android.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApi {
    @GET("/api/v1/households/{householdId}/calendar")
    suspend fun list(
        @Path("householdId") householdId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): List<CalendarEventDto>

    @POST("/api/v1/households/{householdId}/calendar")
    suspend fun create(
        @Path("householdId") householdId: String,
        @Body request: CreateEventRequest
    ): CalendarEventDto

    @PATCH("/api/v1/households/{householdId}/calendar/{eventId}")
    suspend fun update(
        @Path("householdId") householdId: String,
        @Path("eventId") eventId: String,
        @Body request: UpdateEventRequest
    ): CalendarEventDto

    @DELETE("/api/v1/households/{householdId}/calendar/{eventId}")
    suspend fun delete(
        @Path("householdId") householdId: String,
        @Path("eventId") eventId: String
    )
}
