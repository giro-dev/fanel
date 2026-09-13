package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.remote.ChoreDto
import dev.agiro.fanel.android.data.remote.ChoresApi
import dev.agiro.fanel.android.data.remote.CreateChoreRequest
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.RecurrenceFrequency
import dev.agiro.fanel.android.data.remote.UpdateChoreRequest
import dev.agiro.fanel.android.data.remote.UpdateRecurrenceRequest
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

class ChoresViewModel(
    application: Application,
    private val choresApi: ChoresApi = (application as FanelApplication).appContainer.choresApi,
    private val householdApi: HouseholdApi = (application as FanelApplication).appContainer.householdApi,
    private val sessionStore: SessionStore = (application as FanelApplication).appContainer.sessionStore,
    private val householdEvents: HouseholdEvents = (application as FanelApplication).appContainer.householdEvents
) : AndroidViewModel(application) {
    private val householdId = MutableStateFlow("")
    private val _chores = MutableStateFlow<List<ChoreDto>>(emptyList())
    private val _members = MutableStateFlow<List<MemberDto>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ChoresUiState> = combine(
        householdId, _chores, _members, _loading, _errorRes
    ) { currentHouseholdId, chores, members, loading, errorRes ->
        ChoresUiState(
            householdId = currentHouseholdId,
            chores = chores,
            members = members,
            loading = loading,
            errorRes = errorRes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChoresUiState.empty())

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
                .collect { topic -> if (topic == "chores") refresh() }
        }
    }

    fun activateHousehold(newHouseholdId: String) {
        if (householdId.value == newHouseholdId) return
        householdId.value = newHouseholdId
        if (newHouseholdId.isBlank()) return
        viewModelScope.launch {
            _members.value = runCatching { householdApi.members(newHouseholdId) }.getOrDefault(emptyList())
        }
        refresh()
    }

    fun refresh() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        _loading.value = true
        viewModelScope.launch {
            runCatching { choresApi.list(currentHouseholdId) }
                .onSuccess { chores ->
                    _chores.value = chores
                    _errorRes.value = null
                }
                .onFailure { _errorRes.value = R.string.chores_load_error }
            _loading.value = false
        }
    }

    fun createChore(title: String, assigneeId: String?) {
        mutate { choresApi.create(it, CreateChoreRequest(title, assigneeId)) }
    }

    fun toggleDone(chore: ChoreDto) {
        mutate { choresApi.update(it, chore.id, UpdateChoreRequest(done = !chore.done)) }
    }

    fun setAssignee(chore: ChoreDto, assigneeId: String) {
        mutate { choresApi.update(it, chore.id, UpdateChoreRequest(assigneeId = assigneeId)) }
    }

    fun deleteChore(chore: ChoreDto) {
        mutate { choresApi.delete(it, chore.id) }
    }

    fun updateRecurrence(
        chore: ChoreDto,
        dueDate: String?,
        recurrenceFreq: RecurrenceFrequency?,
        recurrenceInterval: Int?,
        rotationMemberIds: List<String>
    ) {
        mutate {
            choresApi.updateRecurrence(
                it, chore.id,
                UpdateRecurrenceRequest(dueDate, recurrenceFreq, recurrenceInterval, rotationMemberIds)
            )
        }
    }

    private fun mutate(block: suspend (String) -> Unit) {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        viewModelScope.launch {
            runCatching { block(currentHouseholdId) }
                .onSuccess { refresh() }
                .onFailure { _errorRes.value = R.string.chores_save_error }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChoresViewModel(application) as T
    }
}

data class ChoresUiState(
    val householdId: String,
    val chores: List<ChoreDto>,
    val members: List<MemberDto>,
    val loading: Boolean,
    val errorRes: Int?
) {
    fun member(memberId: String?): MemberDto? =
        memberId?.let { id -> members.firstOrNull { it.id == id } }

    companion object {
        fun empty(): ChoresUiState = ChoresUiState(
            householdId = "",
            chores = emptyList(),
            members = emptyList(),
            loading = false,
            errorRes = null
        )
    }
}
