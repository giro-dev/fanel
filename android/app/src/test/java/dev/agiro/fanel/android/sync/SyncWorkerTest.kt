package dev.agiro.fanel.android.sync

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import dev.agiro.fanel.android.AppContainerContract
import dev.agiro.fanel.android.AuthStore
import dev.agiro.fanel.android.FanelApplication
import dev.agiro.fanel.android.SessionStore
import dev.agiro.fanel.android.EmptyHouseholdEvents
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.data.remote.AssistantApi
import dev.agiro.fanel.android.data.remote.ChoresApi
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.HouseholdDto
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.MenuApi
import dev.agiro.fanel.android.data.remote.RecipesApi
import dev.agiro.fanel.android.data.remote.ShoppingApi
import dev.agiro.fanel.android.data.remote.VerifyPinRequest
import dev.agiro.fanel.android.data.remote.VerifyPinResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CancellationException

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun rethrowsCancellation() = runBlocking<Unit> {
        val context = testApplication(FakeWorkerRepository(throwable = CancellationException("cancelled")))
        val result = runCatching { workerWith(context).doWork() }
        assertTrue(
            "expected CancellationException but got $result",
            result.exceptionOrNull() is CancellationException
        )
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
        val application = RuntimeEnvironment.getApplication() as FanelApplication
        application.setAppContainerForTest(object : AppContainerContract {
            override val authStore = AuthStore()
            override val sessionStore = SessionStore(application)
            override val householdApi = object : HouseholdApi {
                override suspend fun list(): List<HouseholdDto> = emptyList()
                override suspend fun members(householdId: String): List<MemberDto> = emptyList()
                override suspend fun verifyPin(
                    householdId: String,
                    memberId: String,
                    request: VerifyPinRequest
                ): VerifyPinResponse = VerifyPinResponse(true)
            }
            override val recipesApi: RecipesApi get() = throw UnsupportedOperationException()
            override val menuApi: MenuApi get() = throw UnsupportedOperationException()
            override val shoppingApi: ShoppingApi get() = throw UnsupportedOperationException()
            override val choresApi: ChoresApi get() = throw UnsupportedOperationException()
            override val assistantApi: AssistantApi get() = throw UnsupportedOperationException()
            override val householdEvents: HouseholdEvents = EmptyHouseholdEvents
            override val calendarRepository: CalendarRepositoryContract = repository
            override fun refreshConnection() = Unit
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

    override suspend fun updateEvent(event: CalendarEventEntity, draft: CalendarDraft) = Unit

    override suspend fun deleteEvent(event: CalendarEventEntity) = Unit

    override suspend fun fullSync(householdId: String, from: String, to: String) {
        throwable?.let { throw it }
    }
}
