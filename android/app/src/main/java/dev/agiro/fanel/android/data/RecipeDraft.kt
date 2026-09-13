package dev.agiro.fanel.android.data

data class IngredientDraft(
    val name: String,
    val quantity: Double?,
    val unit: String?
)

data class RecipeDraft(
    val name: String,
    val servings: Int,
    val notes: String?,
    val description: String?,
    val steps: List<String>,
    val ingredients: List<IngredientDraft>,
    val imageMimeType: String?,
    val imageData: String?
)
