package dev.agiro.fanel.android

import android.content.Context
import androidx.room.Room
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.data.local.FanelDatabase
import dev.agiro.fanel.android.data.offline.ChoresRepository
import dev.agiro.fanel.android.data.offline.MembersRepository
import dev.agiro.fanel.android.data.offline.MenuRepository
import dev.agiro.fanel.android.data.offline.OutboxPusher
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.offline.ShoppingRepository
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
import dev.agiro.fanel.android.sync.HouseholdEvents
import dev.agiro.fanel.android.sync.HouseholdSync
import dev.agiro.fanel.android.sync.HouseholdSyncContract
import dev.agiro.fanel.android.sync.SyncSchedulerContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.IOException
import java.util.concurrent.Executor

object EmptyHouseholdEvents : HouseholdEvents {
    override fun observe(householdId: String): Flow<String> = emptyFlow()
}

/** In-memory Room database whose queries run inline so tests can assert right after a call. */
class OfflineTestStore(context: Context) {
    val database: FanelDatabase = Room.inMemoryDatabaseBuilder(context, FanelDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(Executor { it.run() })
        .setTransactionExecutor(Executor { it.run() })
        .build()
    val snapshotDao = database.cachedSnapshotDao()
    val pendingDao = database.pendingOperationDao()
    val pusher = OutboxPusher(pendingDao)
}

class FakeSyncScheduler : SyncSchedulerContract {
    var immediateHouseholdId: String? = null
    var immediateFrom: String? = null
    var immediateTo: String? = null

    override fun enqueuePeriodic(context: Context, householdId: String) = Unit

    override fun enqueueImmediate(context: Context, householdId: String) {
        immediateHouseholdId = householdId
    }

    override fun enqueueImmediate(context: Context, householdId: String, from: String, to: String) {
        immediateHouseholdId = householdId
        immediateFrom = from
        immediateTo = to
    }
}

class FakeHouseholdApi(var members: List<MemberDto> = emptyList()) : HouseholdApi {
    var failMembers = false

    override suspend fun list(): List<HouseholdDto> = emptyList()

    override suspend fun members(householdId: String): List<MemberDto> {
        if (failMembers) throw IOException("offline")
        return members
    }

    override suspend fun verifyPin(
        householdId: String,
        memberId: String,
        request: VerifyPinRequest
    ): VerifyPinResponse = VerifyPinResponse(true)
}

object UnsupportedCalendarRepository : CalendarRepositoryContract {
    override fun observeEvents(householdId: String, from: String, to: String): Flow<List<CalendarEventEntity>> =
        throw UnsupportedOperationException()

    override fun lastSuccessfulSync(householdId: String): Flow<Long> = throw UnsupportedOperationException()

    override suspend fun createEvent(draft: CalendarDraft, householdId: String) = throw UnsupportedOperationException()

    override suspend fun updateEvent(event: CalendarEventEntity, draft: CalendarDraft) = throw UnsupportedOperationException()

    override suspend fun deleteEvent(event: CalendarEventEntity) = throw UnsupportedOperationException()

    override suspend fun fullSync(householdId: String, from: String, to: String) = throw UnsupportedOperationException()
}

/** App container for tests: real members cache over an in-memory database, other domains unsupported. */
class FakeAppContainer(
    context: Context,
    override val sessionStore: SessionStore = SessionStore(context),
    override val householdApi: HouseholdApi = FakeHouseholdApi(),
    override val calendarRepository: CalendarRepositoryContract = UnsupportedCalendarRepository,
    override val householdEvents: HouseholdEvents = EmptyHouseholdEvents
) : AppContainerContract {
    private val store = OfflineTestStore(context)
    override val authStore = AuthStore()
    override val recipesApi: RecipesApi get() = throw UnsupportedOperationException()
    override val menuApi: MenuApi get() = throw UnsupportedOperationException()
    override val shoppingApi: ShoppingApi get() = throw UnsupportedOperationException()
    override val choresApi: ChoresApi get() = throw UnsupportedOperationException()
    override val assistantApi: AssistantApi get() = throw UnsupportedOperationException()
    override val membersRepository: MembersRepository =
        MembersRepository(householdApi, store.snapshotDao, store.pendingDao, store.pusher)
    override val recipesRepository: RecipesRepository get() = throw UnsupportedOperationException()
    override val menuRepository: MenuRepository get() = throw UnsupportedOperationException()
    override val shoppingRepository: ShoppingRepository get() = throw UnsupportedOperationException()
    override val choresRepository: ChoresRepository get() = throw UnsupportedOperationException()
    override val householdSync: HouseholdSyncContract = HouseholdSync(calendarRepository, listOf(membersRepository))

    override fun refreshConnection() = Unit
}
