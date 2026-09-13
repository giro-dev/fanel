package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.RecipeDraft
import dev.agiro.fanel.android.data.remote.CreateRecipeRequest
import dev.agiro.fanel.android.data.remote.RecipeDto
import dev.agiro.fanel.android.data.remote.RecipesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

class RecipesViewModel(
    application: Application,
    private val recipesApi: RecipesApi = (application as FanelApplication).appContainer.recipesApi,
    private val sessionStore: SessionStore = (application as FanelApplication).appContainer.sessionStore
) : AndroidViewModel(application) {
    private val householdId = MutableStateFlow("")
    private val _recipes = MutableStateFlow<List<RecipeDto>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _loading = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<RecipesUiState> = combine(
        householdId, _recipes, _query, _loading, _errorRes
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
            runCatching { recipesApi.list(currentHouseholdId) }
                .onSuccess { result ->
                    _recipes.value = result
                    _errorRes.value = null
                }
                .onFailure { _errorRes.value = R.string.recipes_load_error }
            _loading.value = false
        }
    }

    fun createRecipe(draft: RecipeDraft) {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                recipesApi.create(currentHouseholdId, CreateRecipeRequest.from(draft))
            }
                .onSuccess { refresh() }
                .onFailure { _errorRes.value = R.string.recipes_save_error }
        }
    }

    fun deleteRecipe(recipe: RecipeDto) {
        viewModelScope.launch {
            runCatching { recipesApi.delete(recipe.householdId, recipe.id) }
                .onSuccess { refresh() }
                .onFailure { _errorRes.value = R.string.recipes_save_error }
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
