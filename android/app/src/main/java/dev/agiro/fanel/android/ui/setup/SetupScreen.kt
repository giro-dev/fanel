package dev.agiro.fanel.android.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.SessionViewModel
import dev.agiro.fanel.android.wireguard.PairingError
import dev.agiro.fanel.android.wireguard.PairingViewModel
import androidx.compose.runtime.LaunchedEffect
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import dev.agiro.fanel.android.wireguard.ConnectionError
import dev.agiro.fanel.android.wireguard.ConnectionMode
import java.net.URI

@Composable
fun SetupScreen(
    viewModel: SessionViewModel,
    pairingViewModel: PairingViewModel,
    pairingLink: String?,
    onPairingLinkHandled: () -> Unit,
    onPairingFlowClosed: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pairing by pairingViewModel.state.collectAsStateWithLifecycle()
    val connection by viewModel.connectionStateHolder.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> pairingViewModel.onPermissionResult(result.resultCode == android.app.Activity.RESULT_OK) }
    LaunchedEffect(pairingLink) {
        pairingLink?.let {
            pairingViewModel.receiveLink(it)
            onPairingLinkHandled()
        }
    }
    LaunchedEffect(pairing.paired) {
        if (pairing.paired) viewModel.paired()
    }
    LaunchedEffect(pairing.permissionIntent) {
        pairing.permissionIntent?.let(permissionLauncher::launch)
    }
    val pairingScanner = remember(context) {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    pairingScanner.startScan()
                        .addOnSuccessListener { it.rawValue?.let(pairingViewModel::receiveLink) }
                        .addOnFailureListener { pairingViewModel.reportScannerError() }
                },
                enabled = !pairing.working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.pair_qr_button))
            }
            if (!state.paired) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = viewModel::updateServerUrl,
                    label = { Text(stringResource(R.string.setup_server_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = { Text(stringResource(R.string.setup_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = { Text(stringResource(R.string.setup_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.apiToken,
                onValueChange = viewModel::updateApiToken,
                label = { Text(stringResource(R.string.setup_api_token)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.errorRes?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            pairing.error?.let { error ->
                val message = when (error) {
                    PairingError.INVALID_LINK -> R.string.pair_invalid_link
                    PairingError.LAN_UNREACHABLE -> R.string.pair_wifi_needed
                    PairingError.UNAUTHORIZED -> R.string.pair_auth_error
                    PairingError.GENERAL -> R.string.pair_generic_error
                }
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = {
                    pairingViewModel.clearError()
                    onPairingFlowClosed()
                }) {
                    Text(stringResource(R.string.pair_cancel))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = viewModel::connect,
                enabled = !state.connecting && (!state.paired ||
                    connection.mode == ConnectionMode.LAN || connection.mode == ConnectionMode.REMOTE),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.setup_connect))
                }
            }
            if (state.paired) {
                Text(
                    text = stringResource(connectionModeMessage(connection.mode)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connection.mode == ConnectionMode.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                connection.error?.let { error ->
                    Text(
                        text = stringResource(connectionErrorMessage(error)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    when (error) {
                        ConnectionError.NO_HANDSHAKE -> TextButton(
                            onClick = { pairingViewModel.unpair(viewModel::unpaired) },
                        ) { Text(stringResource(R.string.wg_repair)) }
                        ConnectionError.VPN_PERMISSION -> TextButton(
                            onClick = pairingViewModel::requestVpnPermission,
                        ) { Text(stringResource(R.string.wg_request_permission)) }
                        else -> Unit
                    }
                }
            }

            state.households?.let { households ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.setup_pick_household),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                households.forEach { household ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.selectHousehold(household) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = household.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    pairing.pendingPayload?.let { payload ->
        AlertDialog(
            onDismissRequest = {
                pairingViewModel.dismissConfirmation()
                onPairingFlowClosed()
            },
            title = { Text(stringResource(R.string.pair_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.pair_confirm_message,
                        payload.serverId,
                        runCatching { URI(payload.lanBaseUrl).host }.getOrNull().orEmpty(),
                    ),
                )
            },
            confirmButton = {
                Button(onClick = pairingViewModel::confirmPairing, enabled = !pairing.working) {
                    if (pairing.working) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.pair_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pairingViewModel.dismissConfirmation()
                    onPairingFlowClosed()
                }) {
                    Text(stringResource(R.string.pair_cancel))
                }
            },
        )
    }
}

private fun connectionModeMessage(mode: ConnectionMode): Int = when (mode) {
    ConnectionMode.UNKNOWN -> R.string.wg_mode_unknown
    ConnectionMode.LAN -> R.string.wg_mode_lan
    ConnectionMode.REMOTE -> R.string.wg_mode_remote
    ConnectionMode.OFFLINE -> R.string.wg_mode_offline
    ConnectionMode.ERROR -> R.string.wg_mode_error
}

private fun connectionErrorMessage(error: ConnectionError): Int = when (error) {
    ConnectionError.NO_HANDSHAKE -> R.string.wg_error_no_handshake
    ConnectionError.VPN_PERMISSION -> R.string.wg_error_vpn_permission
    ConnectionError.OTHER_VPN -> R.string.wg_error_other_vpn
    ConnectionError.SERVER_MISMATCH -> R.string.wg_error_server_mismatch
    ConnectionError.UNREACHABLE -> R.string.wg_error_unreachable
}
