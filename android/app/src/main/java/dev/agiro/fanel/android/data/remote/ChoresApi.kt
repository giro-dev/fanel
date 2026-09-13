package dev.agiro.fanel.android.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ChoresApi {
    @GET("/api/v1/households/{householdId}/chores")
    suspend fun list(@Path("householdId") householdId: String): List<ChoreDto>

    @POST("/api/v1/households/{householdId}/chores")
    suspend fun create(
        @Path("householdId") householdId: String,
        @Body request: CreateChoreRequest
    ): ChoreDto

    @PATCH("/api/v1/households/{householdId}/chores/{choreId}")
    suspend fun update(
        @Path("householdId") householdId: String,
        @Path("choreId") choreId: String,
        @Body request: UpdateChoreRequest
    ): ChoreDto

    @PUT("/api/v1/households/{householdId}/chores/{choreId}/recurrence")
    suspend fun updateRecurrence(
        @Path("householdId") householdId: String,
        @Path("choreId") choreId: String,
        @Body request: UpdateRecurrenceRequest
    ): ChoreDto

    @DELETE("/api/v1/households/{householdId}/chores/{choreId}")
    suspend fun delete(
        @Path("householdId") householdId: String,
        @Path("choreId") choreId: String
    )
}
