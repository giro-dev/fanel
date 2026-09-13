package dev.agiro.fanel.android.data.remote

enum class MealType {
    BREAKFAST, LUNCH, SNACK, DINNER
}

data class MealSlotDto(
    val id: String,
    val dayOfWeek: Int,
    val mealType: MealType,
    val text: String?,
    val recipeId: String?
)

data class MealPlanDto(
    val id: String?,
    val householdId: String,
    val isoYear: Int,
    val isoWeek: Int,
    val slots: List<MealSlotDto>?
)
