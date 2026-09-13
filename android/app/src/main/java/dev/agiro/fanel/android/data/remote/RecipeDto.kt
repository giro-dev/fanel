package dev.agiro.fanel.android.data.remote

data class IngredientDto(
    val id: String?,
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val category: String?
)

data class RecipeDto(
    val id: String,
    val householdId: String,
    val name: String,
    val servings: Int,
    val notes: String?,
    val description: String?,
    val steps: List<String>?,
    val tags: List<String>?,
    val imageMimeType: String?,
    val imageData: String?,
    val ingredients: List<IngredientDto>?,
    val createdAt: String?
)
