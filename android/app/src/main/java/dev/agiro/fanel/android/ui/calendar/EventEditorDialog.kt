package dev.agiro.fanel.android.ui.calendar

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.CalendarDraft
import dev.agiro.fanel.android.data.local.CalendarEventEntity
import dev.agiro.fanel.android.data.remote.MemberDto
import java.time.LocalDate
import java.time.LocalTime

private val RECURRENCE_OPTIONS = listOf<String?>(null, "DAILY", "WEEKLY", "MONTHLY", "YEARLY")

@OptIn(ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
fun EventEditorDialog(
    defaultDate: LocalDate,
    initial: CalendarEventEntity?,
    members: List<MemberDto>,
    onDismiss: () -> Unit,
    onConfirm: (CalendarDraft) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var dateText by remember { mutableStateOf((initial?.anchorDate ?: initial?.date) ?: defaultDate.toString()) }
    var timeText by remember { mutableStateOf(initial?.time?.take(5) ?: "") }
    var selectedMembers by remember {
        mutableStateOf(initial?.assigneeIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    var recurrenceFreq by remember { mutableStateOf(initial?.recurrenceFreq) }
    var intervalText by remember { mutableStateOf(initial?.recurrenceInterval?.toString() ?: "1") }
    var untilText by remember { mutableStateOf(initial?.recurrenceUntil ?: "") }
    var error by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val dialogView = LocalView.current

    LaunchedEffect(Unit) {
        (dialogView.parent as? DialogWindowProvider)?.window?.let { window ->
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.create_event else R.string.edit_event))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.event_title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; error = false },
                    label = { Text(stringResource(R.string.event_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it; error = false },
                    label = { Text(stringResource(R.string.event_time_optional)) },
                    placeholder = { Text("18:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (members.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.event_members),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        members.forEach { member ->
                            FilterChip(
                                selected = member.id in selectedMembers,
                                onClick = {
                                    selectedMembers = if (member.id in selectedMembers) {
                                        selectedMembers - member.id
                                    } else {
                                        selectedMembers + member.id
                                    }
                                },
                                label = { Text(member.name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.event_recurrence),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    var freqExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { freqExpanded = true }) {
                        Text(stringResource(recurrenceLabelRes(recurrenceFreq)))
                    }
                    DropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        RECURRENCE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(recurrenceLabelRes(option))) },
                                onClick = {
                                    recurrenceFreq = option
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }

                if (recurrenceFreq != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter(Char::isDigit); error = false },
                        label = { Text(stringResource(R.string.event_recurrence_interval)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = untilText,
                        onValueChange = { untilText = it; error = false },
                        label = { Text(stringResource(R.string.event_recurrence_until)) },
                        placeholder = { Text("2026-12-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error) {
                    Text(
                        text = stringResource(R.string.event_invalid_input),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                    val time = if (timeText.isBlank()) {
                        null
                    } else {
                        runCatching { LocalTime.parse(timeText.trim()) }.getOrNull()
                    }
                    val until = if (untilText.isBlank()) {
                        null
                    } else {
                        runCatching { LocalDate.parse(untilText.trim()) }.getOrNull()
                    }
                    val interval = intervalText.toIntOrNull()
                    val invalid = title.isBlank() || date == null ||
                        (timeText.isNotBlank() && time == null) ||
                        (recurrenceFreq != null && (interval == null || interval < 1)) ||
                        (untilText.isNotBlank() && until == null)
                    if (invalid) {
                        error = true
                    } else {
                        onConfirm(
                            CalendarDraft(
                                title = title.trim(),
                                date = date.toString(),
                                time = time?.toString(),
                                addedBy = initial?.addedBy,
                                assigneeIds = selectedMembers.toList(),
                                recurrenceFreq = recurrenceFreq,
                                recurrenceInterval = if (recurrenceFreq == null) null else interval,
                                recurrenceUntil = if (recurrenceFreq == null) null else until?.toString()
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun recurrenceLabelRes(freq: String?): Int = when (freq) {
    "DAILY" -> R.string.recurrence_daily
    "WEEKLY" -> R.string.recurrence_weekly
    "MONTHLY" -> R.string.recurrence_monthly
    "YEARLY" -> R.string.recurrence_yearly
    else -> R.string.recurrence_none
}
