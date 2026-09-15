package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.offline.MenuRepository
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.remote.MealPlanDto
import dev.agiro.fanel.android.data.remote.MealSlotDto
import dev.agiro.fanel.android.data.remote.MealType
import dev.agiro.fanel.android.data.remote.RecipeDto
import dev.agiro.fanel.android.data.remote.SetSlotRequest
import dev.agiro.fanel.android.sync.HouseholdEvents
import dev.agiro.fanel.android.sync.HouseholdSyncScheduler
import dev.agiro.fanel.android.sync.SyncSchedulerContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.IsoFields

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModel(
    application: Application,
    private val repository: MenuRepository = (application as FanelApplication).appContainer.menuRepository,
    private val recipesRepository: RecipesRepository = (application as FanelApplication).appContainer.recipesRepository,
    private val sessionStore: SessionStore = (application as FanelApplication).appContainer.sessionStore,
    private val householdEvents: HouseholdEvents = (application as FanelApplication).appContainer.householdEvents,
    private val syncScheduler: SyncSchedulerContract = HouseholdSyncScheduler
) : AndroidViewModel(application) {
    private val householdId = MutableStateFlow("")
    private val _referenceDate = MutableStateFlow(LocalDate.now())
    private val _loading = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)

    private val plan: Flow<MealPlanDto?> = combine(householdId, _referenceDate) { id, date -> id to date }
        .flatMapLatest { (id, date) ->
            if (id.isBlank()) flowOf(null) else repository.observeWeek(id, date.isoYear(), date.isoWeek())
        }

    private val recipes: Flow<List<RecipeDto>> = householdId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else recipesRepository.observeRecipes(id)
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MenuUiState> = combine(
        householdId, _referenceDate, plan, recipes, _loading, _errorRes
    ) { values ->
        val currentHouseholdId = values[0] as String
        val date = values[1] as LocalDate
        val plan = values[2] as MealPlanDto?
        val recipes = values[3] as List<RecipeDto>
        val loading = values[4] as Boolean
        val errorRes = values[5] as Int?
        MenuUiState(
            householdId = currentHouseholdId,
            isoYear = date.isoYear(),
            isoWeek = date.isoWeek(),
            weekLabel = getApplication<Application>().getString(
                R.string.menu_week_label,
                date.isoWeek(),
                date.isoYear()
            ),
            slots = plan?.slots.orEmpty(),
            recipes = recipes,
            loading = loading,
            errorRes = errorRes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState.empty())

    init {
        activateHousehold(sessionStore.householdId)
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            householdId
                .flatMapLatest { id ->
                    if (id.isBlank()) emptyFlow() else householdEvents.observe(id)
                }
                .collect { topic -> if (topic == "menu") refresh() }
        }
    }

    fun activateHousehold(newHouseholdId: String) {
        if (householdId.value == newHouseholdId) return
        householdId.value = newHouseholdId
        if (newHouseholdId.isBlank()) return
        viewModelScope.launch { runCatching { recipesRepository.sync(newHouseholdId) } }
        refresh()
    }

    fun refresh() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        val date = _referenceDate.value
        _loading.value = true
        viewModelScope.launch {
            runCatching { repository.syncWeek(currentHouseholdId, date.isoYear(), date.isoWeek()) }
                .onSuccess { _errorRes.value = null }
                .onFailure {
                    val cached = repository.cached(currentHouseholdId, MenuRepository.weekKey(date.isoYear(), date.isoWeek()))
                    if (cached == null) _errorRes.value = R.string.menu_load_error
                }
            _loading.value = false
        }
    }

    fun goToPreviousWeek() {
        _referenceDate.value = _referenceDate.value.minusWeeks(1)
        refresh()
    }

    fun goToNextWeek() {
        _referenceDate.value = _referenceDate.value.plusWeeks(1)
        refresh()
    }

    fun goToToday() {
        _referenceDate.value = LocalDate.now()
        refresh()
    }

    fun setSlot(dayOfWeek: Int, mealType: MealType, text: String?, recipeId: String?) {
        val date = _referenceDate.value
        mutate { id ->
            repository.setSlot(id, date.isoYear(), date.isoWeek(), SetSlotRequest(dayOfWeek, mealType, text, recipeId))
        }
    }

    fun clearSlot(dayOfWeek: Int, mealType: MealType) {
        val date = _referenceDate.value
        mutate { id -> repository.clearSlot(id, date.isoYear(), date.isoWeek(), dayOfWeek, mealType) }
    }

    /** Applies the change to the local cache, then tries to push it; if offline, WorkManager retries later. */
    private fun mutate(block: suspend (String) -> Unit) {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        val date = _referenceDate.value
        viewModelScope.launch {
            runCatching { block(currentHouseholdId) }
                .onFailure { _errorRes.value = R.string.menu_save_error }
                .onSuccess {
                    runCatching { repository.syncWeek(currentHouseholdId, date.isoYear(), date.isoWeek()) }
                        .onFailure { syncScheduler.enqueueImmediate(getApplication(), currentHouseholdId) }
                }
        }
    }

    private fun LocalDate.isoYear(): Int = get(IsoFields.WEEK_BASED_YEAR)

    private fun LocalDate.isoWeek(): Int = get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MenuViewModel(application) as T
    }
}

data class MenuUiState(
    val householdId: String,
    val isoYear: Int,
    val isoWeek: Int,
    val weekLabel: String,
    val slots: List<MealSlotDto>,
    val recipes: List<RecipeDto>,
    val loading: Boolean,
    val errorRes: Int?
) {
    fun slotFor(dayOfWeek: Int, mealType: MealType): MealSlotDto? =
        slots.firstOrNull { it.dayOfWeek == dayOfWeek && it.mealType == mealType }

    fun recipeName(recipeId: String?): String? =
        recipeId?.let { id -> recipes.firstOrNull { it.id == id }?.name }

    companion object {
        fun empty(): MenuUiState = MenuUiState(
            householdId = "",
            isoYear = 0,
            isoWeek = 0,
            weekLabel = "",
            slots = emptyList(),
            recipes = emptyList(),
            loading = false,
            errorRes = null
        )
    }
}
