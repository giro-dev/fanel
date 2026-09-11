package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FanelApplication).appContainer.calendarRepository
    private val householdId = BuildConfig.DEFAULT_HOUSEHOLD_ID
    private val visibleRange = VisibleRange.default()

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.observeEvents(householdId, visibleRange.from, visibleRange.to),
        repository.lastSuccessfulSync().map { lastSync ->
            if (lastSync <= 0L) "—" else java.time.Instant.ofEpochMilli(lastSync).toString()
        }
    ) { events, lastSyncLabel ->
        CalendarUiState(householdId = householdId, events = events, lastSyncLabel = lastSyncLabel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(householdId, emptyList(), "—"))

    init {
        syncNow()
    }

    fun syncNow() {
        if (householdId.isBlank()) return
        viewModelScope.launch {
            repository.fullSync(householdId, visibleRange.from, visibleRange.to)
        }
    }

    fun createSampleEvent() {
        if (householdId.isBlank()) return
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
                householdId = householdId
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
