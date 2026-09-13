package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.remote.MealPlanDto
import dev.agiro.fanel.android.data.remote.MealSlotDto
import dev.agiro.fanel.android.data.remote.MealType
import dev.agiro.fanel.android.data.remote.MenuApi
import dev.agiro.fanel.android.data.remote.RecipeDto
import dev.agiro.fanel.android.data.remote.RecipesApi
import dev.agiro.fanel.android.data.remote.SetSlotRequest
import dev.agiro.fanel.android.sync.HouseholdEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.IsoFields

class MenuViewModel(
    application: Application,
    private val menuApi: MenuApi = (application as FanelApplication).appContainer.menuApi,
    private val recipesApi: RecipesApi = (application as FanelApplication).appContainer.recipesApi,
    private val sessionStore: SessionStore = (application as FanelApplication).appContainer.sessionStore,
    private val householdEvents: HouseholdEvents = (application as FanelApplication).appContainer.householdEvents
) : AndroidViewModel(application) {
    private val householdId = MutableStateFlow("")
    private val _referenceDate = MutableStateFlow(LocalDate.now())
    private val _plan = MutableStateFlow<MealPlanDto?>(null)
    private val _recipes = MutableStateFlow<List<RecipeDto>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MenuUiState> = combine(
        householdId, _referenceDate, _plan, _recipes, _loading, _errorRes
    ) { values ->
        val currentHouseholdId = values[0] as String
        val date = values[1] as LocalDate
        val plan = values[2] as MealPlanDto?
        val recipes = values[3] as List<RecipeDto>
        val loading = values[4] as Boolean
        val errorRes = values[5] as Int?
        MenuUiState(
            householdId = currentHouseholdId,
            isoYear = date.get(IsoFields.WEEK_BASED_YEAR),
            isoWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
            weekLabel = getApplication<Application>().getString(
                R.string.menu_week_label,
                date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                date.get(IsoFields.WEEK_BASED_YEAR)
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

    @OptIn(ExperimentalCoroutinesApi::class)
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
        viewModelScope.launch {
            _recipes.value = runCatching { recipesApi.list(newHouseholdId) }.getOrDefault(emptyList())
        }
        refresh()
    }

    fun refresh() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        val date = _referenceDate.value
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                menuApi.getWeek(
                    currentHouseholdId,
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                )
            }
                .onSuccess { plan ->
                    _plan.value = plan
                    _errorRes.value = null
                }
                .onFailure { _errorRes.value = R.string.menu_load_error }
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
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        val date = _referenceDate.value
        viewModelScope.launch {
            runCatching {
                menuApi.setSlot(
                    currentHouseholdId,
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                    SetSlotRequest(dayOfWeek, mealType, text, recipeId)
                )
            }
                .onSuccess { refresh() }
                .onFailure { _errorRes.value = R.string.menu_save_error }
        }
    }

    fun clearSlot(dayOfWeek: Int, mealType: MealType) {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        val date = _referenceDate.value
        viewModelScope.launch {
            runCatching {
                menuApi.clearSlot(
                    currentHouseholdId,
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                    dayOfWeek,
                    mealType
                )
            }
                .onSuccess { refresh() }
                .onFailure { _errorRes.value = R.string.menu_save_error }
        }
    }

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
