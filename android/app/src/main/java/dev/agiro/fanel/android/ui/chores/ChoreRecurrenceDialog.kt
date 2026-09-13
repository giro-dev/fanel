package dev.agiro.fanel.android.ui.chores

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.remote.ChoreDto
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.RecurrenceFrequency
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoreRecurrenceDialog(
    chore: ChoreDto,
    members: List<MemberDto>,
    onDismiss: () -> Unit,
    onConfirm: (dueDate: String?, freq: RecurrenceFrequency?, interval: Int?, rotationIds: List<String>) -> Unit
) {
    var dueDateText by remember { mutableStateOf(chore.dueDate ?: "") }
    var freq by remember { mutableStateOf(chore.recurrenceFreq) }
    var intervalText by remember { mutableStateOf(chore.recurrenceInterval?.toString() ?: "1") }
    var rotationIds by remember {
        mutableStateOf(chore.rotationMemberIds?.toList() ?: emptyList())
    }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(chore.title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it; error = false },
                    label = { Text(stringResource(R.string.chores_due_date)) },
                    placeholder = { Text("2026-12-31") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.event_recurrence),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    var freqExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { freqExpanded = true }) {
                        Text(
                            stringResource(
                                freq?.let { recurrenceLabelRes(it) } ?: R.string.recurrence_none
                            )
                        )
                    }
                    DropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_none)) },
                            onClick = { freq = null; freqExpanded = false }
                        )
                        RecurrenceFrequency.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(recurrenceLabelRes(option))) },
                                onClick = { freq = option; freqExpanded = false }
                            )
                        }
                    }
                }

                if (freq != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter(Char::isDigit); error = false },
                        label = {
                            Text(
                                stringResource(
                                    R.string.chores_every,
                                    stringResource(repeatUnitRes(freq!!))
                                )
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (members.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.chores_rotation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.chores_rotation_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            members.forEach { member ->
                                val position = rotationIds.indexOf(member.id)
                                FilterChip(
                                    selected = position != -1,
                                    onClick = {
                                        rotationIds = if (position != -1) {
                                            rotationIds - member.id
                                        } else {
                                            rotationIds + member.id
                                        }
                                    },
                                    label = {
                                        Text(
                                            if (position != -1) "${position + 1}. ${member.name}"
                                            else member.name
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (error) {
                    Text(
                        text = stringResource(R.string.chores_invalid_input),
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
                    val dueDate = if (dueDateText.isBlank()) {
                        null
                    } else {
                        runCatching { LocalDate.parse(dueDateText.trim()) }.getOrNull()
                    }
                    val interval = intervalText.toIntOrNull()
                    val invalid = (dueDateText.isNotBlank() && dueDate == null) ||
                        (freq != null && (interval == null || interval < 1))
                    if (invalid) {
                        error = true
                    } else {
                        onConfirm(
                            dueDate?.toString(),
                            freq,
                            if (freq == null) null else interval,
                            if (freq == null) emptyList() else rotationIds
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
