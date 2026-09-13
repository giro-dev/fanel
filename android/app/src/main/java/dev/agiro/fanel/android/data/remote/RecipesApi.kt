package dev.agiro.fanel.android.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RecipesApi {
    @GET("/api/v1/households/{householdId}/recipes")
    suspend fun list(@Path("householdId") householdId: String): List<RecipeDto>

    @GET("/api/v1/households/{householdId}/recipes/{recipeId}")
    suspend fun get(
        @Path("householdId") householdId: String,
        @Path("recipeId") recipeId: String
    ): RecipeDto

    @POST("/api/v1/households/{householdId}/recipes")
    suspend fun create(
        @Path("householdId") householdId: String,
        @Body request: CreateRecipeRequest
    ): RecipeDto

    @DELETE("/api/v1/households/{householdId}/recipes/{recipeId}")
    suspend fun delete(
        @Path("householdId") householdId: String,
        @Path("recipeId") recipeId: String
    )
}
