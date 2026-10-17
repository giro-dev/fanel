package dev.agiro.fanel.android.wireguard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionModeResolverTest {
    @Test
    fun resolvesOfflineLanAndRemoteOnlyForMatchingIdentity() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel()
        val identities = mutableMapOf(
            "https://lan/" to "server-id",
            "https://tunnel/" to "server-id",
        )
        val probes = mutableListOf<String>()
        val holder = ConnectionStateHolder()
        val resolver = resolver(network, tunnel, holder, backgroundScope) { url, _ ->
            probes += url
            identities[url] ?: throw IOException()
        }
        runCurrent()

        network.emit(NetworkSnapshot(available = false, isWifi = false))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ConnectionMode.OFFLINE, holder.state.value.mode)

        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ConnectionMode.LAN, holder.state.value.mode)
        assertEquals(0, tunnel.upCalls)

        identities["https://lan/"] = "another-server"
        network.emit(NetworkSnapshot(available = true, isWifi = false))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ConnectionMode.REMOTE, holder.state.value.mode)
        assertEquals(1, tunnel.upCalls)

        val lanProbes = probes.count { it == "https://lan/" }
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(lanProbes, probes.count { it == "https://lan/" })
        identities["https://tunnel/"] = "another-server"
        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(lanProbes + 1, probes.count { it == "https://lan/" })
        assertEquals(ConnectionMode.ERROR, holder.state.value.mode)
        assertEquals(ConnectionError.SERVER_MISMATCH, holder.state.value.error)
        resolver.stop()
    }

    @Test
    fun retriesFailuresWithBackoffAndSurfacesNoHandshake() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel(TunnelStatus.ERROR(ConnectionError.NO_HANDSHAKE))
        val holder = ConnectionStateHolder()
        var probes = 0
        val resolver = resolver(network, tunnel, holder, backgroundScope) { _, _ ->
            probes++
            throw IOException()
        }
        runCurrent()
        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ConnectionError.NO_HANDSHAKE, holder.state.value.error)
        assertEquals(1, probes)

        for (retrySeconds in listOf(1L, 2L, 4L, 8L, 16L, 30L, 30L)) {
            advanceTimeBy(retrySeconds * 1_000 - 1)
            runCurrent()
            val before = probes
            advanceTimeBy(1)
            runCurrent()
            assertEquals(before + 1, probes)
        }
        resolver.stop()
    }

    @Test
    fun debouncesNetworkChangeBursts() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel()
        val holder = ConnectionStateHolder()
        var probes = 0
        val resolver = resolver(network, tunnel, holder, backgroundScope) { _, _ ->
            probes++
            "server-id"
        }
        runCurrent()

        network.emit(NetworkSnapshot(available = true, isWifi = false))
        network.emit(NetworkSnapshot(available = true, isWifi = true))
        network.emit(NetworkSnapshot(available = true, isWifi = false))
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(1, probes)
        assertEquals(ConnectionMode.LAN, holder.state.value.mode)
        resolver.stop()
    }

    @Test
    fun concurrentNetworkAndForegroundTriggersResolveOnceWithoutInterleaving() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel()
        val holder = ConnectionStateHolder()
        val lanProbeStarted = CompletableDeferred<Unit>()
        val releaseLanProbe = CompletableDeferred<Unit>()
        var activeProbes = 0
        var maxConcurrentProbes = 0
        val resolver = resolver(network, tunnel, holder, backgroundScope) { url, _ ->
            activeProbes++
            maxConcurrentProbes = maxOf(maxConcurrentProbes, activeProbes)
            try {
                if (url == "https://lan/") {
                    lanProbeStarted.complete(Unit)
                    releaseLanProbe.await()
                    throw IOException()
                }
                "server-id"
            } finally {
                activeProbes--
            }
        }
        runCurrent()

        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(lanProbeStarted.isCompleted)
        resolver.onForeground()
        runCurrent()

        releaseLanProbe.complete(Unit)
        runCurrent()

        assertEquals(1, maxConcurrentProbes)
        assertEquals(1, tunnel.upCalls)
        assertEquals(ConnectionMode.REMOTE, holder.state.value.mode)
        resolver.stop()
    }

    @Test
    fun keepsLanTimeoutShortAndAllowsLongerTunnelProbe() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel()
        val holder = ConnectionStateHolder()
        val resolver = resolver(network, tunnel, holder, backgroundScope) { url, _ ->
            if (url == "https://lan/") {
                delay(2_000)
                "another-server"
            } else {
                delay(3_000)
                "server-id"
            }
        }
        runCurrent()

        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        advanceTimeBy(1_499)
        runCurrent()
        assertEquals(0, tunnel.upCalls)

        advanceTimeBy(1)
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(ConnectionMode.REMOTE, holder.state.value.mode)
        assertEquals(1, tunnel.upCalls)
        resolver.stop()
    }

    @Test
    fun unexpectedTunnelFailureFromRemoteEntersBackoffAndRecovers() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel()
        val holder = ConnectionStateHolder()
        val resolver = resolver(network, tunnel, holder, backgroundScope) { url, _ ->
            if (url == "https://lan/") throw IOException()
            "server-id"
        }
        runCurrent()
        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ConnectionMode.REMOTE, holder.state.value.mode)

        tunnel.setStatus(TunnelStatus.ERROR(ConnectionError.UNREACHABLE))
        runCurrent()
        assertEquals(ConnectionMode.ERROR, holder.state.value.mode)
        assertEquals(ConnectionError.UNREACHABLE, holder.state.value.error)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ConnectionMode.REMOTE, holder.state.value.mode)
        assertEquals(2, tunnel.upCalls)
        resolver.stop()
    }

    @Test
    fun backgroundShutdownNeverStartsTunnelFromNetworkCallbacks() = runTest {
        val network = FakeNetwork()
        val tunnel = FakeTunnel()
        val holder = ConnectionStateHolder()
        val resolver = resolver(network, tunnel, holder, backgroundScope) { _, _ -> throw IOException() }
        runCurrent()

        resolver.onBackground()
        network.emit(NetworkSnapshot(available = true, isWifi = true))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0, tunnel.upCalls)
        advanceTimeBy(5 * 60 * 1_000L)
        runCurrent()
        assertEquals(1, tunnel.downCalls)
        assertTrue(holder.state.value.mode == ConnectionMode.UNKNOWN)
        resolver.stop()
    }

    private fun resolver(
        network: FakeNetwork,
        tunnel: FakeTunnel,
        holder: ConnectionStateHolder,
        scope: CoroutineScope,
        probe: suspend (String, String) -> String,
    ) = ConnectionModeResolver(
        network = network,
        prober = IdentityProber(probe),
        tunnel = tunnel,
        config = { testConfig() },
        holder = holder,
        scope = scope,
    )

    private fun testConfig() = WgConfig(
        peerId = "peer-id",
        privateKey = "private-key",
        publicKey = "public-key",
        assignedAddress = "10.8.0.2/32",
        serverPublicKey = "server-public-key",
        presharedKey = null,
        endpoint = "vpn.example.net:51820",
        allowedIps = listOf("10.8.0.0/24"),
        persistentKeepalive = 25,
        tunnelBaseUrl = "https://tunnel/",
        lanBaseUrl = "https://lan/",
        serverId = "server-id",
        spkiSha256 = "pin",
    )

    private class FakeNetwork : NetworkMonitor {
        private val updates = MutableSharedFlow<NetworkSnapshot>(extraBufferCapacity = 8)
        override val snapshots: Flow<NetworkSnapshot> = updates
        suspend fun emit(snapshot: NetworkSnapshot) = updates.emit(snapshot)
    }

    private class FakeTunnel(
        private val nextStatus: TunnelStatus = TunnelStatus.UP,
    ) : TunnelController {
        private val mutableStatus = kotlinx.coroutines.flow.MutableStateFlow<TunnelStatus>(TunnelStatus.DOWN)
        override val status = mutableStatus
        var upCalls = 0
        var downCalls = 0

        override suspend fun up(config: WgConfig): TunnelStatus {
            upCalls++
            mutableStatus.value = nextStatus
            return nextStatus
        }

        override suspend fun down() {
            downCalls++
            mutableStatus.value = TunnelStatus.DOWN
        }

        fun setStatus(status: TunnelStatus) {
            mutableStatus.value = status
        }

        override fun permissionIntent() = null
    }
}
