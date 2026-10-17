package dev.agiro.fanel.android.wireguard

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.crypto.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface TunnelStatus {
    data object DOWN : TunnelStatus
    data object CONNECTING : TunnelStatus
    data object UP : TunnelStatus
    data class ERROR(val reason: ConnectionError) : TunnelStatus
}

interface TunnelController {
    val status: StateFlow<TunnelStatus>
    suspend fun up(config: WgConfig): TunnelStatus
    suspend fun down()
    fun permissionIntent(): Intent?
}

data class TunnelStats(val rxBytes: Long, val txBytes: Long, val lastHandshakeEpochMillis: Long)

fun shouldReresolve(prev: TunnelStats, now: TunnelStats, currentTimeMillis: Long): Boolean =
    now.txBytes > prev.txBytes &&
        now.rxBytes <= prev.rxBytes &&
        currentTimeMillis - now.lastHandshakeEpochMillis > 30_000

class WgTunnelManager(appContext: Context) : TunnelController {
    private val context = appContext.applicationContext
    private val backend by lazy { backend(context) }
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow<TunnelStatus>(TunnelStatus.DOWN)
    override val status: StateFlow<TunnelStatus> = mutableStatus.asStateFlow()
    private val tunnel = object : Tunnel {
        override fun getName() = "fanelwg"
        override fun onStateChange(newState: Tunnel.State) = Unit
    }
    private var activeConfig: WgConfig? = null
    private var watchdog: Job? = null
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun permissionIntent(): Intent? = VpnService.prepare(context)

    override suspend fun up(config: WgConfig): TunnelStatus = mutex.withLock {
        if (permissionIntent() != null) {
            return@withLock update(TunnelStatus.ERROR(ConnectionError.VPN_PERMISSION))
        }
        update(TunnelStatus.CONNECTING)
        try {
            withContext(Dispatchers.IO) {
                backend.setState(tunnel, Tunnel.State.UP, WgConfigFactory.build(config, context.packageName))
            }
            activeConfig = config
            val deadline = System.currentTimeMillis() + 8_000
            while (System.currentTimeMillis() < deadline) {
                if (handshake(config) > 0) {
                    startWatchdog(config)
                    return@withLock update(TunnelStatus.UP)
                }
                delay(500)
            }
            update(TunnelStatus.ERROR(ConnectionError.NO_HANDSHAKE))
        } catch (exception: BackendException) {
            val error = if (exception.reason == BackendException.Reason.VPN_NOT_AUTHORIZED) {
                ConnectionError.VPN_PERMISSION
            } else if (exception.reason == BackendException.Reason.UNABLE_TO_START_VPN) {
                ConnectionError.OTHER_VPN
            } else {
                ConnectionError.UNREACHABLE
            }
            update(TunnelStatus.ERROR(error))
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            update(TunnelStatus.ERROR(ConnectionError.UNREACHABLE))
        }
    }

    override suspend fun down() {
        mutex.withLock {
            watchdog?.cancel()
            watchdog = null
            withContext(Dispatchers.IO) {
                runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            }
            activeConfig = null
            update(TunnelStatus.DOWN)
        }
    }

    private fun startWatchdog(config: WgConfig) {
        watchdog?.cancel()
        watchdog = scope.launch {
            var previous: TunnelStats? = null
            while (true) {
                delay(10_000)
                val current = stats(config)
                if (previous != null && shouldReresolve(previous!!, current, System.currentTimeMillis())) {
                    down()
                    up(config)
                    return@launch
                }
                previous = current
            }
        }
    }

    private suspend fun handshake(config: WgConfig): Long = withContext(Dispatchers.IO) {
        runCatching {
            backend.getStatistics(tunnel).peer(Key.fromBase64(config.serverPublicKey))
                ?.latestHandshakeEpochMillis ?: 0
        }.getOrDefault(0)
    }

    private suspend fun stats(config: WgConfig): TunnelStats = withContext(Dispatchers.IO) {
        runCatching {
            backend.getStatistics(tunnel).peer(Key.fromBase64(config.serverPublicKey))?.let {
                TunnelStats(it.rxBytes, it.txBytes, it.latestHandshakeEpochMillis)
            } ?: TunnelStats(0, 0, 0)
        }.getOrDefault(TunnelStats(0, 0, 0))
    }

    private fun update(value: TunnelStatus): TunnelStatus {
        mutableStatus.value = value
        return value
    }

    private companion object {
        @Volatile private var instance: GoBackend? = null
        fun backend(context: Context): GoBackend = instance ?: synchronized(this) {
            instance ?: GoBackend(context).also { instance = it }
        }
    }
}
