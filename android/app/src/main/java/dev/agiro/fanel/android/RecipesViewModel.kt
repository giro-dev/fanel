package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.RecipeDraft
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.remote.CreateRecipeRequest
import dev.agiro.fanel.android.data.remote.RecipeDto
import dev.agiro.fanel.android.sync.HouseholdSyncScheduler
import dev.agiro.fanel.android.sync.SyncSchedulerContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModel(
    application: Application,
    private val repository: RecipesRepository = (application as FanelApplication).appContainer.recipesRepository,
    private val sessionStore: SessionStore = (application as FanelApplication).appContainer.sessionStore,
    private val syncScheduler: SyncSchedulerContract = HouseholdSyncScheduler
) : AndroidViewModel(application) {
    private val householdId = MutableStateFlow("")
    private val _query = MutableStateFlow("")
    private val _loading = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)

    private val recipes: Flow<List<RecipeDto>> = householdId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.observeRecipes(id)
    }

    val uiState: StateFlow<RecipesUiState> = combine(
        householdId, recipes, _query, _loading, _errorRes
    ) { currentHouseholdId, recipes, query, loading, errorRes ->
        RecipesUiState(
            householdId = currentHouseholdId,
            recipes = filter(recipes, query),
            query = query,
            loading = loading,
            errorRes = errorRes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState.empty())

    init {
        activateHousehold(sessionStore.householdId)
    }

    fun activateHousehold(newHouseholdId: String) {
        if (householdId.value == newHouseholdId) return
        householdId.value = newHouseholdId
        refresh()
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun refresh() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        _loading.value = true
        viewModelScope.launch {
            runCatching { repository.sync(currentHouseholdId) }
                .onSuccess { _errorRes.value = null }
                .onFailure {
                    if (repository.cached(currentHouseholdId) == null) _errorRes.value = R.string.recipes_load_error
                }
            _loading.value = false
        }
    }

    fun createRecipe(draft: RecipeDraft) {
        mutate(householdId.value) { repository.create(it, CreateRecipeRequest.from(draft)) }
    }

    fun deleteRecipe(recipe: RecipeDto) {
        mutate(recipe.householdId) { repository.delete(it, recipe.id) }
    }

    /** Applies the change to the local cache, then tries to push it; if offline, WorkManager retries later. */
    private fun mutate(currentHouseholdId: String, block: suspend (String) -> Unit) {
        if (currentHouseholdId.isBlank()) return
        viewModelScope.launch {
            runCatching { block(currentHouseholdId) }
                .onFailure { _errorRes.value = R.string.recipes_save_error }
                .onSuccess {
                    runCatching { repository.sync(currentHouseholdId) }
                        .onFailure { syncScheduler.enqueueImmediate(getApplication(), currentHouseholdId) }
                }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipesViewModel(application) as T
    }

    companion object {
        fun filter(recipes: List<RecipeDto>, query: String): List<RecipeDto> {
            val q = normalize(query.trim())
            if (q.isEmpty()) return recipes
            return recipes.filter { recipe ->
                val haystack = listOfNotNull(
                    recipe.name,
                    recipe.description,
                    recipe.notes,
                    recipe.ingredients.orEmpty().joinToString(" ") { it.name }
                ).joinToString(" ")
                normalize(haystack).contains(q)
            }
        }

        private fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
    }
}

data class RecipesUiState(
    val householdId: String,
    val recipes: List<RecipeDto>,
    val query: String,
    val loading: Boolean,
    val errorRes: Int?
) {
    companion object {
        fun empty(): RecipesUiState = RecipesUiState(
            householdId = "",
            recipes = emptyList(),
            query = "",
            loading = false,
            errorRes = null
        )
    }
}
