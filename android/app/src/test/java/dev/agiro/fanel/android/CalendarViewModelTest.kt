package dev.agiro.fanel.android

import android.app.Application
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.sync.SyncSchedulerContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class CalendarViewModelTest {
    @Test
    fun createSampleEventDelegatesToRepository() = runBlocking {
        val repository = FakeCalendarRepository()
        val scheduler = FakeSyncScheduler()
        val viewModel = CalendarViewModel(
            application = RuntimeEnvironment.getApplication(),
            repository = repository,
            syncScheduler = scheduler
        )

        viewModel.activateHousehold("household-1")
        viewModel.createSampleEvent()

        assertEquals("household-1", repository.createdHouseholdId)
        assertNotNull(repository.createdDraft)
        assertEquals("Mostra offline", repository.createdDraft?.title)
    }
}

private class FakeCalendarRepository : CalendarRepositoryContract {
    var createdHouseholdId: String? = null
    var createdDraft: CalendarDraft? = null

    override fun observeEvents(householdId: String, from: String, to: String): Flow<List<CalendarEventEntity>> = flowOf(emptyList())

    override fun lastSuccessfulSync(householdId: String): Flow<Long> = flowOf(0L)

    override suspend fun createEvent(draft: CalendarDraft, householdId: String) {
        createdDraft = draft
        createdHouseholdId = householdId
    }

    override suspend fun fullSync(householdId: String, from: String, to: String) = Unit
}

private class FakeSyncScheduler : SyncSchedulerContract {
    override fun enqueuePeriodic(context: android.content.Context, householdId: String) = Unit
    override fun enqueueImmediate(context: android.content.Context, householdId: String) = Unit
}
