package dev.agiro.fanel.android.data.remote

import dev.agiro.fanel.android.data.CalendarDraft

data class CreateEventRequest(
    val title: String,
    val date: String,
    val time: String?,
    val addedBy: String?,
    val assigneeIds: List<String>?,
    val recurrenceFreq: String?,
    val recurrenceInterval: Int?,
    val recurrenceUntil: String?
) {
    companion object {
        fun from(draft: CalendarDraft): CreateEventRequest = CreateEventRequest(
            title = draft.title,
            date = draft.date,
            time = draft.time,
            addedBy = draft.addedBy,
            assigneeIds = draft.assigneeIds.ifEmpty { null },
            recurrenceFreq = draft.recurrenceFreq,
            recurrenceInterval = draft.recurrenceInterval,
            recurrenceUntil = draft.recurrenceUntil
        )
    }
}

data class UpdateEventRequest(
    val title: String?,
    val date: String?,
    val time: String?,
    val assigneeIds: List<String>?,
    val recurrenceFreq: String?,
    val recurrenceInterval: Int?,
    val recurrenceUntil: String?
) {
    companion object {
        fun from(draft: CalendarDraft): UpdateEventRequest = UpdateEventRequest(
            title = draft.title,
            date = draft.date,
            time = draft.time,
            assigneeIds = draft.assigneeIds.ifEmpty { null },
            recurrenceFreq = draft.recurrenceFreq,
            recurrenceInterval = draft.recurrenceInterval,
            recurrenceUntil = draft.recurrenceUntil
        )
    }
}
