package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.offline.ChoresRepository
import dev.agiro.fanel.android.data.offline.MembersRepository
import dev.agiro.fanel.android.data.remote.ChoreDto
import dev.agiro.fanel.android.data.remote.ChoresApi
import dev.agiro.fanel.android.data.remote.CreateChoreRequest
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.RecurrenceFrequency
import dev.agiro.fanel.android.data.remote.UpdateChoreRequest
import dev.agiro.fanel.android.data.remote.UpdateRecurrenceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChoresViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeChoresApi): Pair<ChoresViewModel, MutableList<ChoresUiState>> {
        val context = RuntimeEnvironment.getApplication()
        val sessionStore = SessionStore(context).apply { householdId = "household-1" }
        val store = OfflineTestStore(context)
        val members = FakeHouseholdApi(
            listOf(MemberDto("member-1", "household-1", "Albert", null, "#ff0000", null, null, false, false))
        )
        val vm = ChoresViewModel(
            application = context,
            repository = ChoresRepository(api, store.snapshotDao, store.pendingDao, store.pusher),
            membersRepository = MembersRepository(members, store.snapshotDao, store.pendingDao, store.pusher),
            sessionStore = sessionStore,
            householdEvents = EmptyHouseholdEvents,
            syncScheduler = FakeSyncScheduler()
        )
        return vm to mutableListOf()
    }

    @Test
    fun loadsChoresOnActivation() = runBlocking {
        val api = FakeChoresApi().apply {
            chores.add(chore("1", "Fregar"))
        }
        val (vm, states) = viewModel(api)
        val job = launch(UnconfinedTestDispatcher()) { vm.uiState.collect { states.add(it) } }

        assertEquals("household-1", api.lastListHouseholdId)
        assertEquals(listOf("Fregar"), states.last().chores.map { it.title })
        job.cancel()
    }

    @Test
    fun createChorePostsTitleAndAssignee() = runBlocking {
        val api = FakeChoresApi()
        val (vm, _) = viewModel(api)

        vm.createChore("Escombrar", "member-1")

        assertEquals("Escombrar", api.lastCreateRequest?.title)
        assertEquals("member-1", api.lastCreateRequest?.assigneeId)
        assertEquals(1, api.chores.size)
    }

    @Test
    fun toggleDonePatchesOnlyDone() = runBlocking {
        val api = FakeChoresApi().apply {
            chores.add(chore("1", "Fregar", done = false))
        }
        val (vm, _) = viewModel(api)

        vm.toggleDone(api.chores.first())

        assertEquals("1", api.lastUpdateChoreId)
        assertEquals(true, api.lastUpdateRequest?.done)
        assertTrue(api.lastUpdateRequest?.title == null && api.lastUpdateRequest?.assigneeId == null)
    }

    @Test
    fun updateRecurrencePutsFullConfig() = runBlocking {
        val api = FakeChoresApi().apply {
            chores.add(chore("1", "Treure escombraries"))
        }
        val (vm, _) = viewModel(api)

        vm.updateRecurrence(
            api.chores.first(),
            dueDate = "2026-09-20",
            recurrenceFreq = RecurrenceFrequency.WEEKLY,
            recurrenceInterval = 2,
            rotationMemberIds = listOf("m1", "m2")
        )

        assertEquals("1", api.lastRecurrenceChoreId)
        assertEquals("2026-09-20", api.lastRecurrenceRequest?.dueDate)
        assertEquals(RecurrenceFrequency.WEEKLY, api.lastRecurrenceRequest?.recurrenceFreq)
        assertEquals(2, api.lastRecurrenceRequest?.recurrenceInterval)
        assertEquals(listOf("m1", "m2"), api.lastRecurrenceRequest?.rotationMemberIds)
    }

    private fun chore(id: String, title: String, done: Boolean = false) = ChoreDto(
        id = id,
        householdId = "household-1",
        title = title,
        assigneeId = null,
        done = done,
        createdAt = null,
        dueDate = null,
        recurrenceFreq = null,
        recurrenceInterval = null,
        rotationMemberIds = emptyList()
    )
}

private class FakeChoresApi : ChoresApi {
    var chores: MutableList<ChoreDto> = mutableListOf()
    var lastListHouseholdId: String? = null
    var lastCreateRequest: CreateChoreRequest? = null
    var lastUpdateChoreId: String? = null
    var lastUpdateRequest: UpdateChoreRequest? = null
    var lastRecurrenceChoreId: String? = null
    var lastRecurrenceRequest: UpdateRecurrenceRequest? = null

    override suspend fun list(householdId: String): List<ChoreDto> {
        lastListHouseholdId = householdId
        return chores.toList()
    }

    override suspend fun create(householdId: String, request: CreateChoreRequest): ChoreDto {
        lastCreateRequest = request
        val created = ChoreDto(
            "c-${chores.size + 1}", householdId, request.title, request.assigneeId,
            false, null, request.dueDate, request.recurrenceFreq,
            request.recurrenceInterval, request.rotationMemberIds
        )
        chores.add(created)
        return created
    }

    override suspend fun update(
        householdId: String,
        choreId: String,
        request: UpdateChoreRequest
    ): ChoreDto {
        lastUpdateChoreId = choreId
        lastUpdateRequest = request
        val index = chores.indexOfFirst { it.id == choreId }
        val updated = chores[index].let {
            it.copy(
                title = request.title ?: it.title,
                assigneeId = request.assigneeId ?: it.assigneeId,
                done = request.done ?: it.done
            )
        }
        chores[index] = updated
        return updated
    }

    override suspend fun updateRecurrence(
        householdId: String,
        choreId: String,
        request: UpdateRecurrenceRequest
    ): ChoreDto {
        lastRecurrenceChoreId = choreId
        lastRecurrenceRequest = request
        val index = chores.indexOfFirst { it.id == choreId }
        val updated = chores[index].copy(
            dueDate = request.dueDate,
            recurrenceFreq = request.recurrenceFreq,
            recurrenceInterval = request.recurrenceInterval,
            rotationMemberIds = request.rotationMemberIds
        )
        chores[index] = updated
        return updated
    }

    override suspend fun delete(householdId: String, choreId: String) {
        chores.removeAll { it.id == choreId }
    }
}
