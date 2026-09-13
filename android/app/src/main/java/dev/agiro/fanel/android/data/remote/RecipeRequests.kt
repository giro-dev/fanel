package dev.agiro.fanel.android.data.remote

import dev.agiro.fanel.android.data.RecipeDraft

data class CreateRecipeRequest(
    val name: String,
    val servings: Int,
    val notes: String?,
    val description: String?,
    val steps: List<String>,
    val tags: List<String>,
    val ingredients: List<IngredientDto>,
    val imageMimeType: String?,
    val imageData: String?
) {
    companion object {
        fun from(draft: RecipeDraft): CreateRecipeRequest = CreateRecipeRequest(
            name = draft.name,
            servings = draft.servings,
            notes = draft.notes,
            description = draft.description,
            steps = draft.steps,
            tags = emptyList(),
            ingredients = draft.ingredients.map {
                IngredientDto(
                    id = null,
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit,
                    category = null
                )
            },
            imageMimeType = draft.imageMimeType,
            imageData = draft.imageData
        )
    }
}
