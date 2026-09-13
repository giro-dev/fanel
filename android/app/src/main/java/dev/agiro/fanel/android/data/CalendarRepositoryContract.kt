package dev.agiro.fanel.android.data

import dev.agiro.fanel.android.data.local.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

interface CalendarRepositoryContract {
    fun observeEvents(householdId: String, from: String, to: String): Flow<List<CalendarEventEntity>>
    fun lastSuccessfulSync(householdId: String): Flow<Long>
    suspend fun createEvent(draft: CalendarDraft, householdId: String)
    suspend fun updateEvent(event: CalendarEventEntity, draft: CalendarDraft)
    suspend fun deleteEvent(event: CalendarEventEntity)
    suspend fun fullSync(householdId: String, from: String, to: String)
}
