package dev.agiro.fanel.android.wireguard

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.AppContainerContract
import dev.agiro.fanel.android.FanelApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class GeneratedWgKey(val privateKey: String, val publicKey: String)

fun interface WgKeyGenerator {
    fun generate(): GeneratedWgKey
}

fun interface PairingEnrollmentClient {
    suspend fun enroll(payload: PairingPayload, publicKey: String, deviceName: String): WgEnrollResponse
}

enum class PairingError { INVALID_LINK, LAN_UNREACHABLE, UNAUTHORIZED, GENERAL }

data class PairingUiState(
    val pendingPayload: PairingPayload? = null,
    val working: Boolean = false,
    val paired: Boolean = false,
    val error: PairingError? = null,
    val permissionIntent: Intent? = null,
)

class PairingViewModel(
    private val enrollment: PairingEnrollmentClient,
    private val configRepository: WgConfigRepository,
    private val tunnel: TunnelController,
    private val revokePeer: suspend (String) -> Unit,
    private val enterPairedMode: () -> Unit,
    private val startConnectionResolution: () -> Unit,
    private val unpairMode: () -> Unit,
    private val keyGenerator: WgKeyGenerator = WgKeyGenerator {
        val pair = com.wireguard.crypto.KeyPair()
        GeneratedWgKey(pair.privateKey.toBase64(), pair.publicKey.toBase64())
    },
) : ViewModel() {
    private val mutableState = MutableStateFlow(PairingUiState(paired = configRepository.current != null))
    val state: StateFlow<PairingUiState> = mutableState.asStateFlow()

    fun receiveLink(link: String) {
        try {
            mutableState.update {
                it.copy(pendingPayload = PairingPayload.parse(link), error = null)
            }
        } catch (_: InvalidPairingPayload) {
            mutableState.update { it.copy(error = PairingError.INVALID_LINK) }
        }
    }

    fun dismissConfirmation() {
        mutableState.update { it.copy(pendingPayload = null, error = null) }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    fun reportScannerError() {
        mutableState.update { it.copy(error = PairingError.GENERAL) }
    }

    fun confirmPairing() {
        val payload = mutableState.value.pendingPayload ?: return
        if (mutableState.value.working) return
        mutableState.update { it.copy(working = true, error = null, permissionIntent = null) }
        viewModelScope.launch {
            try {
                val key = keyGenerator.generate()
                val enrolled = enrollment.enroll(
                    payload,
                    key.publicKey,
                    Build.MODEL.orEmpty(),
                )
                val config = WgConfig(
                    peerId = enrolled.peerId,
                    privateKey = key.privateKey,
                    publicKey = key.publicKey,
                    assignedAddress = enrolled.assignedAddress,
                    serverPublicKey = enrolled.serverPublicKey,
                    presharedKey = enrolled.presharedKey,
                    endpoint = enrolled.endpoint,
                    allowedIps = enrolled.allowedIps,
                    persistentKeepalive = enrolled.persistentKeepalive,
                    tunnelBaseUrl = enrolled.tunnelBaseUrl,
                    lanBaseUrl = enrolled.lanBaseUrl,
                    serverId = payload.serverId,
                    spkiSha256 = payload.spkiSha256,
                )
                configRepository.save(config)
                enterPairedMode()
                val permission = tunnel.permissionIntent()
                mutableState.update {
                    it.copy(
                        working = false,
                        paired = true,
                        pendingPayload = null,
                        permissionIntent = permission,
                    )
                }
                if (permission == null) startConnectionResolution()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                mutableState.update {
                    it.copy(working = false, error = if (exception.code() == 401) {
                        PairingError.UNAUTHORIZED
                    } else {
                        PairingError.GENERAL
                    })
                }
            } catch (_: IOException) {
                mutableState.update { it.copy(working = false, error = PairingError.LAN_UNREACHABLE) }
            } catch (_: Exception) {
                mutableState.update { it.copy(working = false, error = PairingError.GENERAL) }
            }
        }
    }

    fun onPermissionResult(@Suppress("UNUSED_PARAMETER") granted: Boolean) {
        mutableState.update { it.copy(permissionIntent = null) }
        if (configRepository.current != null) startConnectionResolution()
    }

    fun requestVpnPermission() {
        val intent = tunnel.permissionIntent()
        if (intent == null) {
            startConnectionResolution()
        } else {
            mutableState.update { it.copy(permissionIntent = intent) }
        }
    }

    fun unpair(onComplete: () -> Unit = {}) {
        val config = configRepository.current
        viewModelScope.launch {
            if (config != null) {
                runCatching { revokePeer(config.publicKey) }
            }
            tunnel.down()
            configRepository.clear()
            unpairMode()
            mutableState.value = PairingUiState()
            onComplete()
        }
    }

    class Factory(
        private val application: Application,
        private val container: AppContainerContract =
            (application as FanelApplication).appContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val client = PairingEnrollmentClient { payload, publicKey, deviceName ->
                pinnedWgApi(payload.lanBaseUrl, payload.spkiSha256).enroll(
                    WgEnrollRequest(payload.token, publicKey, deviceName),
                )
            }
            return PairingViewModel(
                enrollment = client,
                configRepository = container.wgConfigRepository,
                tunnel = container.tunnelController,
                revokePeer = container::revokeWgPeer,
                enterPairedMode = container::enterPairedMode,
                startConnectionResolution = container::startConnectionResolution,
                unpairMode = container::unpair,
            ) as T
        }
    }
}
