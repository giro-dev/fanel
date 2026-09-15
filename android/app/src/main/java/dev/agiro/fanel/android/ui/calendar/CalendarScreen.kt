package dev.agiro.fanel.android.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.agiro.fanel.android.CalendarViewMode
import dev.agiro.fanel.android.CalendarViewModel
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.ui.components.OfflineBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editCandidate by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<CalendarEventEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_screen_title)) },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync_now))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_event))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineBanner()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.goToPrevious() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_period)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.periodLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.lastSyncLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.goToToday() }) {
                        Text(stringResource(R.string.today))
                    }
                    IconButton(onClick = { viewModel.goToNext() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_period)
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                CalendarViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CalendarViewMode.entries.size
                        )
                    ) {
                        Text(stringResource(mode.labelRes()))
                    }
                }
            }

            if (state.householdId.isBlank()) {
                Text(
                    stringResource(R.string.household_unconfigured_status),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            when (state.viewMode) {
                CalendarViewMode.DAY -> CalendarDayView(
                    date = state.referenceDate,
                    events = state.events,
                    onEventClick = { editCandidate = it },
                    onEventLongClick = { deleteCandidate = it }
                )
                CalendarViewMode.WEEK -> CalendarWeekView(
                    referenceDate = state.referenceDate,
                    events = state.events,
                    onDayClick = { viewModel.selectDate(it); viewModel.setViewMode(CalendarViewMode.DAY) },
                    onEventClick = { editCandidate = it },
                    onEventLongClick = { deleteCandidate = it }
                )
                CalendarViewMode.MONTH -> CalendarMonthView(
                    referenceDate = state.referenceDate,
                    events = state.events,
                    onDayClick = { viewModel.selectDate(it); viewModel.setViewMode(CalendarViewMode.DAY) }
                )
            }
        }
    }

    if (showCreateDialog || editCandidate != null) {
        val editing = editCandidate
        EventEditorDialog(
            defaultDate = state.referenceDate,
            initial = editing,
            members = members,
            onDismiss = {
                showCreateDialog = false
                editCandidate = null
            },
            onDelete = editing?.let { event ->
                {
                    editCandidate = null
                    deleteCandidate = event
                }
            },
            onConfirm = { draft ->
                if (editing == null) {
                    viewModel.createEvent(draft)
                } else {
                    viewModel.updateEvent(editing, draft)
                }
                showCreateDialog = false
                editCandidate = null
            }
        )
    }

    deleteCandidate?.let { event ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.delete_event_title)) },
            text = { Text(stringResource(R.string.delete_event_message, event.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEvent(event)
                    deleteCandidate = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun CalendarViewMode.labelRes(): Int = when (this) {
    CalendarViewMode.DAY -> R.string.view_day
    CalendarViewMode.WEEK -> R.string.view_week
    CalendarViewMode.MONTH -> R.string.view_month
}
