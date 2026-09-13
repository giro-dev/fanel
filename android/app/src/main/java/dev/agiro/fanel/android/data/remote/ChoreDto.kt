package dev.agiro.fanel.android.data.remote

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

data class ChoreDto(
    val id: String,
    val householdId: String,
    val title: String,
    val assigneeId: String?,
    val done: Boolean,
    val createdAt: String?,
    val dueDate: String?,
    val recurrenceFreq: RecurrenceFrequency?,
    val recurrenceInterval: Int?,
    val rotationMemberIds: List<String>?
)
