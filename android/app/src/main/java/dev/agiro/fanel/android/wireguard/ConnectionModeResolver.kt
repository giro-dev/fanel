package dev.agiro.fanel.android.wireguard

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NetworkSnapshot(val available: Boolean, val isWifi: Boolean)

interface NetworkMonitor {
    val snapshots: Flow<NetworkSnapshot>
}

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    override val snapshots: Flow<NetworkSnapshot> = callbackFlow {
        fun snapshot(network: Network?) {
            val caps = network?.let(connectivity::getNetworkCapabilities)
            trySend(
                NetworkSnapshot(
                    available = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                    isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
                ),
            )
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = snapshot(network)
            override fun onLost(network: Network) = snapshot(connectivity.activeNetwork)
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = snapshot(network)
        }
        connectivity.registerDefaultNetworkCallback(callback)
        snapshot(connectivity.activeNetwork)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
}

fun interface IdentityProber {
    suspend fun identity(baseUrl: String, pin: String): String
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
class ConnectionModeResolver(
    network: NetworkMonitor,
    private val prober: IdentityProber,
    private val tunnel: TunnelController,
    private val config: () -> WgConfig?,
    private val holder: ConnectionStateHolder,
    private val scope: CoroutineScope,
) {
    @Volatile private var foreground = true
    @Volatile private var active = true
    private val resolveMutex = Mutex()
    private var retryJob: Job? = null
    private var backgroundJob: Job? = null
    @Volatile
    private var latestNetwork = NetworkSnapshot(false, false)

    private val networkJob = network.snapshots
        .debounce(1_000)
        .distinctUntilChanged()
        .onEach { latestNetwork = it; resolve() }
        .launchIn(scope)
    private val tunnelStatusJob = tunnel.status
        .drop(1)
        .onEach(::onTunnelStatusChanged)
        .launchIn(scope)

    fun stop() {
        active = false
        networkJob.cancel()
        tunnelStatusJob.cancel()
        retryJob?.cancel()
        backgroundJob?.cancel()
    }

    fun onForeground() {
        if (!active) return
        foreground = true
        backgroundJob?.cancel()
        scope.launch { resolve() }
    }

    fun onBackground() {
        if (!active) return
        foreground = false
        backgroundJob?.cancel()
        backgroundJob = scope.launch {
            delay(5 * 60 * 1000L)
            if (!foreground) {
                holder.set(ConnectionState(ConnectionMode.UNKNOWN))
                tunnel.down()
            }
        }
    }

    private suspend fun resolve(retrying: Boolean = false) = resolveMutex.withLock {
        if (!active) return@withLock
        if (!retrying) {
            retryJob?.cancel()
            retryJob = null
        }
        val current = config() ?: return@withLock
        if (!latestNetwork.available) {
            holder.set(ConnectionState(ConnectionMode.OFFLINE))
            tunnel.down()
            return@withLock
        }
        if (!foreground) return@withLock
        try {
            val lan = withTimeout(1_500) { prober.identity(current.lanBaseUrl, current.spkiSha256) }
            if (lan == current.serverId) {
                holder.set(ConnectionState(ConnectionMode.LAN))
                tunnel.down()
                return@withLock
            }
        } catch (_: TimeoutCancellationException) {
            Unit
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Unit
        }
        if (!active || !foreground) return@withLock
        val tunnelStatus = if (tunnel.status.value == TunnelStatus.UP) {
            TunnelStatus.UP
        } else {
            tunnel.up(current)
        }
        if (tunnelStatus != TunnelStatus.UP) {
            val error = (tunnelStatus as? TunnelStatus.ERROR)?.reason ?: ConnectionError.NO_HANDSHAKE
            fail(error, retrying)
            return@withLock
        }
        try {
            val remote = withTimeout(5_000) { prober.identity(current.tunnelBaseUrl, current.spkiSha256) }
            if (!active) return@withLock
            if (remote == current.serverId) {
                holder.set(ConnectionState(ConnectionMode.REMOTE))
            } else {
                fail(ConnectionError.SERVER_MISMATCH, retrying)
            }
        } catch (_: TimeoutCancellationException) {
            fail(ConnectionError.UNREACHABLE, retrying)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            fail(ConnectionError.UNREACHABLE, retrying)
        }
    }

    private suspend fun onTunnelStatusChanged(status: TunnelStatus) {
        val error = when (status) {
            TunnelStatus.DOWN -> ConnectionError.NO_HANDSHAKE
            is TunnelStatus.ERROR -> status.reason
            else -> return
        }
        resolveMutex.withLock {
            if (active && holder.state.value.mode == ConnectionMode.REMOTE) {
                fail(error, retrying = false)
            }
        }
    }

    private fun fail(error: ConnectionError, retrying: Boolean) {
        holder.set(ConnectionState(ConnectionMode.ERROR, error))
        if (retrying) return
        retryJob?.cancel()
        retryJob = scope.launch {
            var seconds = 1L
            while (foreground) {
                delay(seconds * 1_000)
                resolve(retrying = true)
                if (holder.state.value.mode != ConnectionMode.ERROR) return@launch
                seconds = (seconds * 2).coerceAtMost(30)
            }
        }
    }
}
