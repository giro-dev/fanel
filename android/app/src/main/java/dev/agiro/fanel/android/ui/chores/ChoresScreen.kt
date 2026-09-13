package dev.agiro.fanel.android.ui.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.agiro.fanel.android.ChoresViewModel
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.remote.ChoreDto
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.RecurrenceFrequency
import dev.agiro.fanel.android.ui.components.OfflineBanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreen(viewModel: ChoresViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newTitle by remember { mutableStateOf("") }
    var newAssigneeId by remember { mutableStateOf<String?>(null) }
    var recurrenceTarget by remember { mutableStateOf<ChoreDto?>(null) }
    var deleteCandidate by remember { mutableStateOf<ChoreDto?>(null) }

    val quickAdd: () -> Unit = {
        if (newTitle.isNotBlank()) {
            viewModel.createChore(newTitle.trim(), newAssigneeId)
            newTitle = ""
            newAssigneeId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chores_screen_title)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync_now))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
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
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text(stringResource(R.string.chores_add_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { quickAdd() }),
                    modifier = Modifier.weight(1f)
                )
                AssigneePicker(
                    members = state.members,
                    selectedId = newAssigneeId,
                    onSelect = { newAssigneeId = it }
                )
                IconButton(onClick = quickAdd, enabled = newTitle.isNotBlank()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chores_add)
                    )
                }
            }

            if (state.householdId.isBlank()) {
                Text(
                    stringResource(R.string.household_unconfigured_status),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.errorRes?.let { errorRes ->
                Text(
                    stringResource(errorRes),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(state.chores, key = { it.id }) { chore ->
                    ChoreRow(
                        chore = chore,
                        assignee = state.member(chore.assigneeId),
                        onToggleDone = { viewModel.toggleDone(chore) },
                        onEditRecurrence = { recurrenceTarget = chore },
                        onRemove = { deleteCandidate = chore }
                    )
                }
                if (state.chores.isEmpty() && !state.loading && state.errorRes == null) {
                    item {
                        Text(
                            stringResource(R.string.chores_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    recurrenceTarget?.let { chore ->
        ChoreRecurrenceDialog(
            chore = chore,
            members = state.members,
            onDismiss = { recurrenceTarget = null },
            onConfirm = { dueDate, freq, interval, rotationIds ->
                viewModel.updateRecurrence(chore, dueDate, freq, interval, rotationIds)
                recurrenceTarget = null
            }
        )
    }

    deleteCandidate?.let { chore ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.chores_delete_title)) },
            text = { Text(stringResource(R.string.chores_delete_message, chore.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChore(chore)
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

@Composable
private fun ChoreRow(
    chore: ChoreDto,
    assignee: MemberDto?,
    onToggleDone: () -> Unit,
    onEditRecurrence: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = chore.done, onCheckedChange = { onToggleDone() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chore.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (chore.done) TextDecoration.LineThrough else TextDecoration.None
            )
            val meta = listOfNotNull(
                chore.dueDate?.let { formatDate(it) },
                chore.recurrenceFreq?.let { freq ->
                    val interval = chore.recurrenceInterval ?: 1
                    if (interval > 1) {
                        stringResource(R.string.chores_every_n, interval, stringResource(repeatUnitRes(freq)))
                    } else {
                        stringResource(recurrenceLabelRes(freq))
                    }
                }
            ).joinToString(" · ")
            if (meta.isNotBlank() || assignee != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    assignee?.let { MemberChip(it) }
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        IconButton(onClick = onEditRecurrence) {
            Icon(
                Icons.Filled.Repeat,
                contentDescription = stringResource(R.string.event_recurrence),
                tint = if (chore.recurrenceFreq != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MemberChip(member: MemberDto) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 6.dp)
    ) {
        member.color?.let { hex ->
            runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()?.let { color ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
        }
        Text(
            member.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun AssigneePicker(
    members: List<MemberDto>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = members.firstOrNull { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.name ?: stringResource(R.string.chores_unassigned))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chores_unassigned)) },
                onClick = { onSelect(null); expanded = false }
            )
            members.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.name) },
                    onClick = { onSelect(member.id); expanded = false }
                )
            }
        }
    }
}

private fun formatDate(iso: String): String =
    runCatching {
        LocalDate.parse(iso).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        )
    }.getOrDefault(iso)

fun recurrenceLabelRes(freq: RecurrenceFrequency): Int = when (freq) {
    RecurrenceFrequency.DAILY -> R.string.recurrence_daily
    RecurrenceFrequency.WEEKLY -> R.string.recurrence_weekly
    RecurrenceFrequency.MONTHLY -> R.string.recurrence_monthly
    RecurrenceFrequency.YEARLY -> R.string.recurrence_yearly
}

fun repeatUnitRes(freq: RecurrenceFrequency): Int = when (freq) {
    RecurrenceFrequency.DAILY -> R.string.chores_unit_days
    RecurrenceFrequency.WEEKLY -> R.string.chores_unit_weeks
    RecurrenceFrequency.MONTHLY -> R.string.chores_unit_months
    RecurrenceFrequency.YEARLY -> R.string.chores_unit_years
}
