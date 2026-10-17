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
import androidx.compose.material3.Button
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
import dev.agiro.fanel.android.wireguard.ConnectionError
import dev.agiro.fanel.android.wireguard.ConnectionMode
import dev.agiro.fanel.android.wireguard.ConnectionStateHolder
import dev.agiro.fanel.android.wireguard.PairingViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    viewModel: SessionViewModel,
    pairingViewModel: PairingViewModel,
    connectionStateHolder: ConnectionStateHolder,
    onDismiss: () -> Unit,
) {
    var households by remember { mutableStateOf<List<HouseholdDto>?>(null) }
    var members by remember { mutableStateOf<List<MemberDto>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connection by connectionStateHolder.state.collectAsStateWithLifecycle()
    val pairing by pairingViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmUnpair by remember { mutableStateOf(false) }

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
                if (state.paired) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(connectionModeString(connection.mode)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    connection.error?.let { error ->
                        Text(
                            text = stringResource(connectionErrorString(error)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (connection.error == ConnectionError.NO_HANDSHAKE) {
                        TextButton(onClick = { confirmUnpair = true }) {
                            Text(stringResource(R.string.wg_repair))
                        }
                    }
                    Button(
                        onClick = { confirmUnpair = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.wg_unpair))
                    }
                }
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

    if (confirmUnpair && pairing.paired) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text(stringResource(R.string.wg_unpair_title)) },
            text = { Text(stringResource(R.string.wg_unpair_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    pairingViewModel.unpair {
                        viewModel.unpaired()
                        onDismiss()
                    }
                }) {
                    Text(stringResource(R.string.wg_unpair))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnpair = false }) {
                    Text(stringResource(R.string.pair_cancel))
                }
            },
        )
    }
}

private fun connectionModeString(mode: ConnectionMode): Int = when (mode) {
    ConnectionMode.UNKNOWN -> R.string.wg_mode_unknown
    ConnectionMode.LAN -> R.string.wg_mode_lan
    ConnectionMode.REMOTE -> R.string.wg_mode_remote
    ConnectionMode.OFFLINE -> R.string.wg_mode_offline
    ConnectionMode.ERROR -> R.string.wg_mode_error
}

private fun connectionErrorString(error: ConnectionError): Int = when (error) {
    ConnectionError.NO_HANDSHAKE -> R.string.wg_error_no_handshake
    ConnectionError.VPN_PERMISSION -> R.string.wg_error_vpn_permission
    ConnectionError.OTHER_VPN -> R.string.wg_error_other_vpn
    ConnectionError.SERVER_MISMATCH -> R.string.wg_error_server_mismatch
    ConnectionError.UNREACHABLE -> R.string.wg_error_unreachable
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
