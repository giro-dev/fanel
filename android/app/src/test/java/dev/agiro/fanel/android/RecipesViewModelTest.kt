package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.IngredientDraft
import dev.agiro.fanel.android.data.RecipeDraft
import dev.agiro.fanel.android.data.offline.RecipesRepository
import dev.agiro.fanel.android.data.remote.CreateRecipeRequest
import dev.agiro.fanel.android.data.remote.RecipeDto
import dev.agiro.fanel.android.data.remote.RecipesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecipesViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeRecipesApi, householdId: String = "household-1"): RecipesViewModel {
        val context = RuntimeEnvironment.getApplication()
        val sessionStore = SessionStore(context).apply { this.householdId = householdId }
        val store = OfflineTestStore(context)
        return RecipesViewModel(
            application = context,
            repository = RecipesRepository(api, store.snapshotDao, store.pendingDao, store.pusher),
            sessionStore = sessionStore,
            syncScheduler = FakeSyncScheduler()
        )
    }

    @Test
    fun loadsRecipesOnActivation() = runBlocking {
        val api = FakeRecipesApi().apply {
            recipes = mutableListOf(recipe("1", "Arròs"), recipe("2", "Truita"))
        }
        val vm = viewModel(api)
        val states = mutableListOf<RecipesUiState>()
        val job = launch(UnconfinedTestDispatcher()) { vm.uiState.collect { states.add(it) } }

        assertEquals("household-1", api.lastListHouseholdId)
        assertEquals(listOf("Arròs", "Truita"), states.last().recipes.map { it.name })
        job.cancel()
    }

    @Test
    fun filtersIgnoringCaseAndDiacritics() {
        val recipes = listOf(recipe("1", "Arròs caldós"), recipe("2", "Truita de patates"))

        assertEquals(listOf("Arròs caldós"), RecipesViewModel.filter(recipes, "arros").map { it.name })
        assertEquals(listOf("Truita de patates"), RecipesViewModel.filter(recipes, "PATATES").map { it.name })
        assertEquals(2, RecipesViewModel.filter(recipes, "").size)
    }

    @Test
    fun createRecipePostsAndRefreshes() = runBlocking {
        val api = FakeRecipesApi()
        val vm = viewModel(api)
        val states = mutableListOf<RecipesUiState>()
        val job = launch(UnconfinedTestDispatcher()) { vm.uiState.collect { states.add(it) } }

        vm.createRecipe(
            RecipeDraft(
                name = "Pa amb tomàquet",
                servings = 2,
                notes = null,
                description = null,
                steps = listOf("Fregar el tomàquet"),
                ingredients = listOf(IngredientDraft("pa", 2.0, "llesques")),
                imageMimeType = null,
                imageData = null
            )
        )

        assertEquals("household-1", api.lastCreateHouseholdId)
        assertEquals("Pa amb tomàquet", api.lastCreateRequest?.name)
        assertEquals(1, api.lastCreateRequest?.ingredients?.size)
        assertEquals(listOf("Pa amb tomàquet"), states.last().recipes.map { it.name })
        job.cancel()
    }

    @Test
    fun deleteRecipeRemovesAndRefreshes() = runBlocking {
        val api = FakeRecipesApi().apply {
            recipes = mutableListOf(recipe("1", "Arròs"))
        }
        val vm = viewModel(api)
        val states = mutableListOf<RecipesUiState>()
        val job = launch(UnconfinedTestDispatcher()) { vm.uiState.collect { states.add(it) } }

        vm.deleteRecipe(api.recipes.first())

        assertEquals("1", api.lastDeleteRecipeId)
        assertEquals(0, api.recipes.size)
        assertEquals(0, states.last().recipes.size)
        job.cancel()
    }

    private fun recipe(id: String, name: String) = RecipeDto(
        id = id,
        householdId = "household-1",
        name = name,
        servings = 4,
        notes = null,
        description = null,
        steps = emptyList(),
        tags = emptyList(),
        imageMimeType = null,
        imageData = null,
        ingredients = emptyList(),
        createdAt = null
    )
}

private class FakeRecipesApi : RecipesApi {
    var recipes: MutableList<RecipeDto> = mutableListOf()
    var lastListHouseholdId: String? = null
    var lastCreateHouseholdId: String? = null
    var lastCreateRequest: CreateRecipeRequest? = null
    var lastDeleteRecipeId: String? = null

    override suspend fun list(householdId: String): List<RecipeDto> {
        lastListHouseholdId = householdId
        return recipes.toList()
    }

    override suspend fun get(householdId: String, recipeId: String): RecipeDto =
        recipes.first { it.id == recipeId }

    override suspend fun create(householdId: String, request: CreateRecipeRequest): RecipeDto {
        lastCreateHouseholdId = householdId
        lastCreateRequest = request
        val created = RecipeDto(
            id = "new-${recipes.size + 1}",
            householdId = householdId,
            name = request.name,
            servings = request.servings,
            notes = request.notes,
            description = request.description,
            steps = request.steps,
            tags = request.tags,
            imageMimeType = request.imageMimeType,
            imageData = request.imageData,
            ingredients = request.ingredients,
            createdAt = null
        )
        recipes.add(created)
        return created
    }

    override suspend fun delete(householdId: String, recipeId: String) {
        lastDeleteRecipeId = recipeId
        recipes.removeAll { it.id == recipeId }
    }
}
