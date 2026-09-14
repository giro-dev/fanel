package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.offline.MenuRepository
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.remote.CreateRecipeRequest
import dev.agiro.fanel.android.data.remote.MealPlanDto
import dev.agiro.fanel.android.data.remote.MealSlotDto
import dev.agiro.fanel.android.data.remote.MealType
import dev.agiro.fanel.android.data.remote.MenuApi
import dev.agiro.fanel.android.data.remote.RecipeDto
import dev.agiro.fanel.android.data.remote.RecipesApi
import dev.agiro.fanel.android.data.remote.SetSlotRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.temporal.IsoFields

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MenuViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(menuApi: FakeMenuApi, recipesApi: RecipesApi = EmptyRecipesApi): MenuViewModel {
        val context = RuntimeEnvironment.getApplication()
        val sessionStore = SessionStore(context).apply { householdId = "household-1" }
        val store = OfflineTestStore(context)
        return MenuViewModel(
            application = context,
            repository = MenuRepository(menuApi, store.snapshotDao, store.pendingDao, store.pusher),
            recipesRepository = RecipesRepository(recipesApi, store.snapshotDao, store.pendingDao, store.pusher),
            sessionStore = sessionStore,
            householdEvents = EmptyHouseholdEvents,
            syncScheduler = FakeSyncScheduler()
        )
    }

    private fun currentIsoWeek(): Pair<Int, Int> {
        val now = LocalDate.now()
        return now.get(IsoFields.WEEK_BASED_YEAR) to now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    }

    @Test
    fun loadsCurrentWeekOnActivation() = runBlocking {
        val menuApi = FakeMenuApi()
        val vm = viewModel(menuApi)
        val states = mutableListOf<MenuUiState>()
        val job = launch(UnconfinedTestDispatcher()) { vm.uiState.collect { states.add(it) } }

        val (year, week) = currentIsoWeek()
        assertEquals(year to week, menuApi.lastGetYear to menuApi.lastGetWeek)
        assertEquals("household-1", menuApi.lastGetHouseholdId)
        assertEquals(year, states.last().isoYear)
        assertEquals(week, states.last().isoWeek)
        job.cancel()
    }

    @Test
    fun weekNavigationShiftsIsoWeek() = runBlocking {
        val menuApi = FakeMenuApi()
        val vm = viewModel(menuApi)

        vm.goToNextWeek()
        val nextWeek = LocalDate.now().plusWeeks(1)
        assertEquals(
            nextWeek.get(IsoFields.WEEK_BASED_YEAR) to nextWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
            menuApi.lastGetYear to menuApi.lastGetWeek
        )

        vm.goToPreviousWeek()
        vm.goToPreviousWeek()
        val previousWeek = LocalDate.now().minusWeeks(1)
        assertEquals(
            previousWeek.get(IsoFields.WEEK_BASED_YEAR) to previousWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
            menuApi.lastGetYear to menuApi.lastGetWeek
        )
    }

    @Test
    fun setSlotPutsRequestAndRefreshes() = runBlocking {
        val menuApi = FakeMenuApi()
        val vm = viewModel(menuApi)
        val (year, week) = currentIsoWeek()

        vm.setSlot(dayOfWeek = 3, mealType = MealType.DINNER, text = "Pizza", recipeId = null)

        assertEquals(year to week, menuApi.lastSetYear to menuApi.lastSetWeek)
        assertEquals(3, menuApi.lastSetRequest?.dayOfWeek)
        assertEquals(MealType.DINNER, menuApi.lastSetRequest?.mealType)
        assertEquals("Pizza", menuApi.lastSetRequest?.text)
        assertNull(menuApi.lastSetRequest?.recipeId)
    }

    @Test
    fun clearSlotDeletesAndRefreshes() = runBlocking {
        val menuApi = FakeMenuApi()
        val vm = viewModel(menuApi)
        val (year, week) = currentIsoWeek()

        vm.clearSlot(dayOfWeek = 5, mealType = MealType.LUNCH)

        assertEquals(year to week, menuApi.lastClearYear to menuApi.lastClearWeek)
        assertEquals(5, menuApi.lastClearDay)
        assertEquals(MealType.LUNCH, menuApi.lastClearMealType)
    }
}

private class FakeMenuApi : MenuApi {
    var lastGetHouseholdId: String? = null
    var lastGetYear: Int = 0
    var lastGetWeek: Int = 0
    var lastSetYear: Int = 0
    var lastSetWeek: Int = 0
    var lastSetRequest: SetSlotRequest? = null
    var lastClearYear: Int = 0
    var lastClearWeek: Int = 0
    var lastClearDay: Int = 0
    var lastClearMealType: MealType? = null
    var plan: MealPlanDto = MealPlanDto(null, "household-1", 0, 0, emptyList())

    override suspend fun getWeek(householdId: String, year: Int, week: Int): MealPlanDto {
        lastGetHouseholdId = householdId
        lastGetYear = year
        lastGetWeek = week
        return plan
    }

    override suspend fun setSlot(
        householdId: String,
        year: Int,
        week: Int,
        request: SetSlotRequest
    ): MealSlotDto {
        lastSetYear = year
        lastSetWeek = week
        lastSetRequest = request
        return MealSlotDto("slot-1", request.dayOfWeek, request.mealType, request.text, request.recipeId)
    }

    override suspend fun clearSlot(
        householdId: String,
        year: Int,
        week: Int,
        dayOfWeek: Int,
        mealType: MealType
    ) {
        lastClearYear = year
        lastClearWeek = week
        lastClearDay = dayOfWeek
        lastClearMealType = mealType
    }
}

private object EmptyRecipesApi : RecipesApi {
    override suspend fun list(householdId: String): List<RecipeDto> = emptyList()
    override suspend fun get(householdId: String, recipeId: String): RecipeDto =
        throw NoSuchElementException()
    override suspend fun create(householdId: String, request: CreateRecipeRequest): RecipeDto =
        throw UnsupportedOperationException()
    override suspend fun delete(householdId: String, recipeId: String) = Unit
}
