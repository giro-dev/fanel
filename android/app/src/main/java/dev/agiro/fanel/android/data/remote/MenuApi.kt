package dev.agiro.fanel.android.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MenuApi {
    @GET("/api/v1/households/{householdId}/menu")
    suspend fun getWeek(
        @Path("householdId") householdId: String,
        @Query("year") year: Int,
        @Query("week") week: Int
    ): MealPlanDto

    @PUT("/api/v1/households/{householdId}/menu/slots")
    suspend fun setSlot(
        @Path("householdId") householdId: String,
        @Query("year") year: Int,
        @Query("week") week: Int,
        @Body request: SetSlotRequest
    ): MealSlotDto

    @DELETE("/api/v1/households/{householdId}/menu/slots")
    suspend fun clearSlot(
        @Path("householdId") householdId: String,
        @Query("year") year: Int,
        @Query("week") week: Int,
        @Query("dayOfWeek") dayOfWeek: Int,
        @Query("mealType") mealType: MealType
    )
}
