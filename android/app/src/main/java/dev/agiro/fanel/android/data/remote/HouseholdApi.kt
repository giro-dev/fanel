package dev.agiro.fanel.android.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class HouseholdDto(
    val id: String,
    val name: String,
    val locale: String?,
    val timezone: String?,
    val createdAt: String?
)

data class MemberDto(
    val id: String,
    val householdId: String,
    val name: String,
    val role: String?,
    val color: String?,
    val username: String?,
    val guardianIds: List<String>?,
    val hasPin: Boolean,
    val hasCredentials: Boolean
)

data class VerifyPinRequest(val pin: String)

data class VerifyPinResponse(val valid: Boolean)

interface HouseholdApi {
    @GET("/api/v1/households")
    suspend fun list(): List<HouseholdDto>

    @GET("/api/v1/households/{householdId}/members")
    suspend fun members(@Path("householdId") householdId: String): List<MemberDto>

    @POST("/api/v1/households/{householdId}/members/{memberId}/verify-pin")
    suspend fun verifyPin(
        @Path("householdId") householdId: String,
        @Path("memberId") memberId: String,
        @Body request: VerifyPinRequest
    ): VerifyPinResponse
}
