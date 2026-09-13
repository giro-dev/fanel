package dev.agiro.fanel.android.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AssistantApi {
    @GET("/api/v1/households/{householdId}/assistant/agents")
    suspend fun agents(@Path("householdId") householdId: String): List<AgentDto>

    @POST("/api/v1/households/{householdId}/assistant/chat")
    suspend fun chat(
        @Path("householdId") householdId: String,
        @Body request: AgentRequest
    ): AgentResponse
}
