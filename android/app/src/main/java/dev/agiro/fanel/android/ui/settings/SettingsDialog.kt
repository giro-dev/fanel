package dev.agiro.fanel.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.SessionViewModel
import dev.agiro.fanel.android.data.remote.HouseholdDto
import dev.agiro.fanel.android.data.remote.MemberDto
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(viewModel: SessionViewModel, onDismiss: () -> Unit) {
    var households by remember { mutableStateOf<List<HouseholdDto>?>(null) }
    var members by remember { mutableStateOf<List<MemberDto>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        households = runCatching { viewModel.listHouseholds() }
            .onFailure { loadFailed = true }
            .getOrNull()
        members = runCatching { viewModel.listMembers() }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_current_household, state.householdName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider()
                when {
                    households == null && !loadFailed -> CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                    loadFailed -> Text(
                        text = stringResource(R.string.setup_connection_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(households.orEmpty()) { household ->
                            ListItem(
                                headlineContent = { Text(household.name) },
                                modifier = Modifier.clickable {
                                    viewModel.selectHousehold(household)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                members?.let { memberList ->
                    if (memberList.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = stringResource(R.string.whoami_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = state.memberName.ifBlank {
                                stringResource(R.string.whoami_none)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        WhoAmIPicker(
                            members = memberList,
                            currentMemberId = state.memberId,
                            onPick = { member, pin -> viewModel.pickMember(member, pin) },
                            onClear = { viewModel.clearMember() },
                            scope = scope
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                viewModel.logout()
                onDismiss()
            }) {
                Text(
                    stringResource(R.string.settings_logout),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
private fun WhoAmIPicker(
    members: List<MemberDto>,
    currentMemberId: String,
    onPick: suspend (MemberDto, String?) -> Boolean,
    onClear: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var pendingMember by remember { mutableStateOf<MemberDto?>(null) }
    var pin by remember { mutableStateOf("") }
    var wrongPin by remember { mutableStateOf(false) }

    Column {
        members.forEach { member ->
            val isCurrent = member.id == currentMemberId
            ListItem(
                headlineContent = { Text(member.name) },
                supportingContent = if (isCurrent) {
                    { Text(stringResource(R.string.whoami_current)) }
                } else null,
                modifier = Modifier.clickable(enabled = !isCurrent) {
                    if (member.hasPin) {
                        pendingMember = member
                        pin = ""
                        wrongPin = false
                    } else {
                        scope.launch { onPick(member, null) }
                    }
                }
            )
        }

        pendingMember?.let { member ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; wrongPin = false },
                    label = { Text(stringResource(R.string.whoami_pin, member.name)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    scope.launch {
                        val ok = onPick(member, pin)
                        if (ok) {
                            pendingMember = null
                            pin = ""
                        } else {
                            wrongPin = true
                        }
                    }
                }) {
                    Text(stringResource(R.string.whoami_confirm))
                }
            }
            if (wrongPin) {
                Text(
                    text = stringResource(R.string.whoami_wrong_pin),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (currentMemberId.isNotBlank()) {
            TextButton(onClick = {
                onClear()
                pendingMember = null
            }) {
                Text(stringResource(R.string.whoami_forget))
            }
        }
    }
}
