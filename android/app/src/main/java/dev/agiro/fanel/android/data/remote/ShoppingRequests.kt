package dev.agiro.fanel.android.data.remote

data class ListNameRequest(val name: String)

data class AddItemRequest(
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val category: String?,
    val recurring: Boolean
)

data class UpdateItemRequest(
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val category: String? = null,
    val recurring: Boolean? = null,
    val done: Boolean? = null
)
