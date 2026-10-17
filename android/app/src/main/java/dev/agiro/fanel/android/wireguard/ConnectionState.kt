package dev.agiro.fanel.android.wireguard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionMode { UNKNOWN, LAN, REMOTE, OFFLINE, ERROR }
enum class ConnectionError { NO_HANDSHAKE, VPN_PERMISSION, OTHER_VPN, SERVER_MISMATCH, UNREACHABLE }
data class ConnectionState(val mode: ConnectionMode, val error: ConnectionError? = null)

class ConnectionStateHolder(initial: ConnectionState = ConnectionState(ConnectionMode.UNKNOWN)) {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<ConnectionState> = mutableState.asStateFlow()

    fun set(value: ConnectionState) {
        mutableState.value = value
    }
}
