package dev.agiro.fanel.android.data.remote

data class CreateChoreRequest(
    val title: String,
    val assigneeId: String?,
    val dueDate: String? = null,
    val recurrenceFreq: RecurrenceFrequency? = null,
    val recurrenceInterval: Int? = null,
    val rotationMemberIds: List<String>? = null
)

data class UpdateChoreRequest(
    val title: String? = null,
    val assigneeId: String? = null,
    val done: Boolean? = null
)

data class UpdateRecurrenceRequest(
    val dueDate: String?,
    val recurrenceFreq: RecurrenceFrequency?,
    val recurrenceInterval: Int?,
    val rotationMemberIds: List<String>
)
