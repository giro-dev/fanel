package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.CalendarRepositoryContract
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.sync.CalendarSyncScheduler
import dev.agiro.fanel.android.sync.HouseholdEvents
import dev.agiro.fanel.android.sync.SyncSchedulerContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class CalendarViewMode {
    DAY, WEEK, MONTH
}

class CalendarViewModel(
    application: Application,
    private val repository: CalendarRepositoryContract = (application as FanelApplication).appContainer.calendarRepository,
    private val syncScheduler: SyncSchedulerContract = CalendarSyncScheduler,
    private val householdApi: HouseholdApi = (application as FanelApplication).appContainer.householdApi,
    private val householdEvents: HouseholdEvents = (application as FanelApplication).appContainer.householdEvents
) : AndroidViewModel(application) {
    private val householdId = MutableStateFlow("")
    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    private val _referenceDate = MutableStateFlow(LocalDate.now())
    private val _members = MutableStateFlow<List<MemberDto>>(emptyList())
    val viewMode: StateFlow<CalendarViewMode> = _viewMode
    val referenceDate: StateFlow<LocalDate> = _referenceDate
    val members: StateFlow<List<MemberDto>> = _members

    private val visibleRange = combine(_viewMode, _referenceDate) { mode, date ->
        rangeFor(mode, date)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val events = combine(householdId, visibleRange, ::Pair)
        .flatMapLatest { (id, range) -> repository.observeEvents(id, range.first, range.second) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastSync = householdId.flatMapLatest { repository.lastSuccessfulSync(it) }

    val uiState: StateFlow<CalendarUiState> = combine(
        householdId,
        events,
        _viewMode,
        _referenceDate,
        lastSync.map { timestamp ->
            if (timestamp <= 0L) {
                getApplication<Application>().getString(R.string.last_sync_never)
            } else {
                timestampFormatter
                    .withLocale(Locale.getDefault())
                    .format(java.time.Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
            }
        }
    ) { currentHouseholdId, currentEvents, mode, date, lastSyncLabel ->
        CalendarUiState(
            householdId = currentHouseholdId,
            events = currentEvents,
            viewMode = mode,
            referenceDate = date,
            periodLabel = periodLabel(mode, date),
            lastSyncLabel = lastSyncLabel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState.empty())

    init {
        activateHousehold(
            (application as FanelApplication).appContainer.sessionStore.householdId
        )
        subscribeToEvents()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun subscribeToEvents() {
        viewModelScope.launch {
            householdId
                .flatMapLatest { id ->
                    if (id.isBlank()) emptyFlow() else householdEvents.observe(id)
                }
                .collect { topic -> if (topic == "calendar") syncVisibleRange() }
        }
    }

    fun activateHousehold(newHouseholdId: String) {
        if (householdId.value == newHouseholdId) return
        householdId.value = newHouseholdId
        if (newHouseholdId.isBlank()) return
        syncScheduler.enqueuePeriodic(getApplication(), newHouseholdId)
        syncVisibleRange()
        viewModelScope.launch {
            _members.value = runCatching { householdApi.members(newHouseholdId) }.getOrDefault(emptyList())
        }
    }

    fun setViewMode(mode: CalendarViewMode) {
        if (_viewMode.value == mode) return
        _viewMode.value = mode
        syncVisibleRange()
    }

    fun goToToday() {
        _referenceDate.value = LocalDate.now()
        syncVisibleRange()
    }

    fun goToPrevious() {
        _referenceDate.value = when (_viewMode.value) {
            CalendarViewMode.DAY -> _referenceDate.value.minusDays(1)
            CalendarViewMode.WEEK -> _referenceDate.value.minusWeeks(1)
            CalendarViewMode.MONTH -> _referenceDate.value.minusMonths(1)
        }
        syncVisibleRange()
    }

    fun goToNext() {
        _referenceDate.value = when (_viewMode.value) {
            CalendarViewMode.DAY -> _referenceDate.value.plusDays(1)
            CalendarViewMode.WEEK -> _referenceDate.value.plusWeeks(1)
            CalendarViewMode.MONTH -> _referenceDate.value.plusMonths(1)
        }
        syncVisibleRange()
    }

    fun selectDate(date: LocalDate) {
        _referenceDate.value = date
    }

    fun syncNow() {
        syncVisibleRange()
    }

    fun createEvent(draft: CalendarDraft) {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        viewModelScope.launch {
            repository.createEvent(draft, currentHouseholdId)
            syncVisibleRange()
        }
    }

    fun updateEvent(event: CalendarEventEntity, draft: CalendarDraft) {
        viewModelScope.launch {
            repository.updateEvent(event, draft)
            syncVisibleRange()
        }
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            syncVisibleRange()
        }
    }

    private fun syncVisibleRange() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        val (from, to) = rangeFor(_viewMode.value, _referenceDate.value)
        syncScheduler.enqueueImmediate(getApplication(), currentHouseholdId, from, to)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(application) as T
    }

    companion object {
        fun rangeFor(mode: CalendarViewMode, date: LocalDate): Pair<String, String> = when (mode) {
            CalendarViewMode.DAY -> date.toString() to date.toString()
            CalendarViewMode.WEEK -> {
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                monday.toString() to monday.plusDays(6).toString()
            }
            CalendarViewMode.MONTH -> {
                val first = date.withDayOfMonth(1)
                first.toString() to first.plusMonths(1).minusDays(1).toString()
            }
        }

        private fun periodLabel(mode: CalendarViewMode, date: LocalDate): String {
            val locale = Locale.getDefault()
            return when (mode) {
                CalendarViewMode.DAY ->
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)
                CalendarViewMode.WEEK -> {
                    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val sunday = monday.plusDays(6)
                    val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                    "${day.format(monday)} – ${day.format(sunday)}"
                }
                CalendarViewMode.MONTH ->
                    DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(date)
            }
        }
    }
}

data class CalendarUiState(
    val householdId: String,
    val events: List<CalendarEventEntity>,
    val viewMode: CalendarViewMode,
    val referenceDate: LocalDate,
    val periodLabel: String,
    val lastSyncLabel: String
) {
    companion object {
        fun empty(): CalendarUiState = CalendarUiState(
            householdId = "",
            events = emptyList(),
            viewMode = CalendarViewMode.MONTH,
            referenceDate = LocalDate.now(),
            periodLabel = "",
            lastSyncLabel = ""
        )
    }
}

private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withZone(ZoneId.systemDefault())
