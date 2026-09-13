package dev.agiro.fanel.android.data

data class CalendarDraft(
    val title: String,
    val date: String,
    val time: String?,
    val addedBy: String?,
    val assigneeIds: List<String>,
    val recurrenceFreq: String?,
    val recurrenceInterval: Int?,
    val recurrenceUntil: String?
)
