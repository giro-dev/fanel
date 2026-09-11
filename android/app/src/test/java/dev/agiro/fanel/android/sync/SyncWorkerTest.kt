package dev.agiro.fanel.android.sync

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import dev.agiro.fanel.android.AppContainerContract
import dev.agiro.fanel.android.AuthStore
import dev.agiro.fanel.android.FanelApplication
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CancellationException

class SyncWorkerTest {
    @Test
    fun returnsFailureWhenInputIsInvalid() = runBlocking {
        val context = testApplication(FakeWorkerRepository())
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setInputData(Data.Builder().build())
            .build()

        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun returnsSuccessWhenSyncSucceeds() = runBlocking {
        val context = testApplication(FakeWorkerRepository())
        val worker = workerWith(context)

        assertEquals(Result.success(), worker.doWork())
    }

    @Test
    fun returnsFailureForConfigurationErrors() = runBlocking {
        val context = testApplication(FakeWorkerRepository(throwable = IllegalArgumentException("bad config")))
        val worker = workerWith(context)

        assertEquals(Result.failure(), worker.doWork())
    }

    @Test
    fun returnsRetryForTransientErrors() = runBlocking {
        val context = testApplication(FakeWorkerRepository(throwable = RuntimeException("boom")))
        val worker = workerWith(context)

        assertEquals(Result.retry(), worker.doWork())
    }

    @Test(expected = CancellationException::class)
    fun rethrowsCancellation() = runBlocking {
        val context = testApplication(FakeWorkerRepository(throwable = CancellationException("cancelled")))
        workerWith(context).doWork()
    }

    private fun workerWith(context: Context): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setInputData(
                Data.Builder()
                    .putString(SyncWorker.KEY_HOUSEHOLD_ID, "household-1")
                    .putString(SyncWorker.KEY_FROM, "2026-01-01")
                    .putString(SyncWorker.KEY_TO, "2026-01-31")
                    .build()
            )
            .build()

    private fun testApplication(repository: FakeWorkerRepository): Context {
        val application = RuntimeEnvironment.getApplication<FanelApplication>()
        application.setAppContainerForTest(object : AppContainerContract {
            override val authStore = AuthStore()
            override val calendarRepository: CalendarRepositoryContract = repository
        })
        return application
    }
}

private class FakeWorkerRepository(
    private val throwable: Throwable? = null
) : CalendarRepositoryContract {
    override fun observeEvents(householdId: String, from: String, to: String): Flow<List<CalendarEventEntity>> = flowOf(emptyList())

    override fun lastSuccessfulSync(householdId: String): Flow<Long> = flowOf(0L)

    override suspend fun createEvent(draft: CalendarDraft, householdId: String) = Unit

    override suspend fun fullSync(householdId: String, from: String, to: String) {
        throwable?.let { throw it }
    }
}
