package dev.agiro.fanel.android.data.remote

data class ShoppingItemDto(
    val id: String,
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val category: String?,
    val recurring: Boolean,
    val done: Boolean
)

data class ShoppingListDto(
    val id: String,
    val householdId: String,
    val name: String,
    val items: List<ShoppingItemDto>?
)
