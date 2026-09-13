package dev.agiro.fanel.android.data.remote

data class SetSlotRequest(
    val dayOfWeek: Int,
    val mealType: MealType,
    val text: String?,
    val recipeId: String?
)
