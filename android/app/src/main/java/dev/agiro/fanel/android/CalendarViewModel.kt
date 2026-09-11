package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.sync.CalendarSyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FanelApplication).appContainer.calendarRepository
    private val householdId = MutableStateFlow("")
    private val visibleRange = VisibleRange.default()
    private val events = householdId.flatMapLatest { repository.observeEvents(it, visibleRange.from, visibleRange.to) }

    val uiState: StateFlow<CalendarUiState> = combine(
        householdId,
        events,
        repository.lastSuccessfulSync().map { lastSync ->
            if (lastSync <= 0L) {
                getApplication<Application>().getString(R.string.last_sync_never)
            } else {
                timestampFormatter
                    .withLocale(Locale.getDefault())
                    .format(java.time.Instant.ofEpochMilli(lastSync))
            }
        }
    ) { currentHouseholdId, events, lastSyncLabel ->
        CalendarUiState(householdId = currentHouseholdId, events = events, lastSyncLabel = lastSyncLabel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState("", emptyList(), ""))

    init {
        activateHousehold(BuildConfig.DEFAULT_HOUSEHOLD_ID)
    }

    fun activateHousehold(newHouseholdId: String) {
        householdId.value = newHouseholdId
        if (newHouseholdId.isBlank()) return
        CalendarSyncScheduler.enqueuePeriodic(getApplication(), newHouseholdId)
        syncNow()
    }

    fun syncNow() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        viewModelScope.launch {
            repository.fullSync(currentHouseholdId, visibleRange.from, visibleRange.to)
        }
    }

    fun createSampleEvent() {
        val currentHouseholdId = householdId.value
        if (currentHouseholdId.isBlank()) return
        viewModelScope.launch {
            repository.createEvent(
                draft = CalendarDraft(
                    title = getApplication<Application>().getString(R.string.sample_event_title),
                    date = LocalDate.now().plusDays(1).toString(),
                    time = LocalTime.of(18, 0).toString(),
                    addedBy = null,
                    assigneeIds = emptyList(),
                    recurrenceFreq = null,
                    recurrenceInterval = null,
                    recurrenceUntil = null
                ),
                householdId = currentHouseholdId
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(application) as T
    }
}

data class CalendarUiState(
    val householdId: String,
    val events: List<CalendarEventEntity>,
    val lastSyncLabel: String
)

data class VisibleRange(val from: String, val to: String) {
    companion object {
        fun default(): VisibleRange {
            val today = LocalDate.now()
            return VisibleRange(today.minusMonths(1).toString(), today.plusMonths(3).toString())
        }
    }
}

private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withZone(ZoneId.systemDefault())
