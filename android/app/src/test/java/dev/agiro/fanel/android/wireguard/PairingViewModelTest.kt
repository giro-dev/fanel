package dev.agiro.fanel.android.wireguard

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setMainDispatcher() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun enrollsAndStoresConfigThenStartsConnectionResolution() = runTest(dispatcher) {
        val store = FakeConfigRepository()
        var pairedModeEntered = false
        var resolutionStarted = false
        val viewModel = PairingViewModel(
            enrollment = PairingEnrollmentClient { _, publicKey, _ ->
                assertNotNull(publicKey)
                response()
            },
            configRepository = store,
            tunnel = FakeTunnel(),
            revokePeer = {},
            enterPairedMode = { pairedModeEntered = true },
            startConnectionResolution = { resolutionStarted = true },
            unpairMode = {},
            keyGenerator = { GeneratedWgKey("private", "public") },
        )

        viewModel.receiveLink(pairingLink())
        viewModel.confirmPairing()
        advanceUntilIdle()

        assertEquals(true, pairedModeEntered)
        assertEquals(true, resolutionStarted)
        assertEquals("peer-id", store.current?.peerId)
        assertEquals("private", store.current?.privateKey)
        assertEquals(true, viewModel.state.value.paired)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun reportsLanUnreachableWithoutPersistingAnEnrollment() = runTest(dispatcher) {
        val store = FakeConfigRepository()
        val viewModel = viewModel(store) { _, _, _ -> throw IOException("network unavailable") }

        viewModel.receiveLink(pairingLink())
        viewModel.confirmPairing()
        advanceUntilIdle()

        assertEquals(PairingError.LAN_UNREACHABLE, viewModel.state.value.error)
        assertNull(store.current)
    }

    @Test
    fun reportsUnauthorizedEnrollment() = runTest(dispatcher) {
        val store = FakeConfigRepository()
        val viewModel = viewModel(store) { _, _, _ ->
            throw HttpException(Response.error<WgEnrollResponse>(
                401,
                "Unauthorized".toResponseBody(),
            ))
        }

        viewModel.receiveLink(pairingLink())
        viewModel.confirmPairing()
        advanceUntilIdle()

        assertEquals(PairingError.UNAUTHORIZED, viewModel.state.value.error)
        assertNull(store.current)
    }

    private fun viewModel(
        store: FakeConfigRepository,
        enroll: suspend (PairingPayload, String, String) -> WgEnrollResponse,
    ) = PairingViewModel(
        enrollment = PairingEnrollmentClient(enroll),
        configRepository = store,
        tunnel = FakeTunnel(),
        revokePeer = {},
        enterPairedMode = {},
        startConnectionResolution = {},
        unpairMode = {},
        keyGenerator = { GeneratedWgKey("private", "public") },
    )

    private fun response() = WgEnrollResponse(
        peerId = "peer-id",
        assignedAddress = "10.8.0.2/32",
        serverPublicKey = "server-public",
        presharedKey = null,
        endpoint = "vpn.example.net:51820",
        allowedIps = listOf("10.8.0.0/24"),
        persistentKeepalive = 25,
        tunnelBaseUrl = "https://10.8.0.1:8443",
        lanBaseUrl = "https://fanel.home:8443",
    )

    private fun pairingLink(): String {
        val json = """{"v":1,"token":"one-time","serverId":"server-id","lanBaseUrl":"https://fanel.home:8443","tunnelBaseUrl":"https://10.8.0.1:8443","spkiSha256":"${Base64.getEncoder().encodeToString(ByteArray(32))}"}"""
        return "fanel://pair?d=${Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())}"
    }

    private class FakeConfigRepository : WgConfigRepository {
        override var current: WgConfig? = null
            private set

        override suspend fun save(c: WgConfig) {
            current = c
        }

        override suspend fun clear() {
            current = null
        }
    }

    private class FakeTunnel : TunnelController {
        private val mutableStatus =
            kotlinx.coroutines.flow.MutableStateFlow<TunnelStatus>(TunnelStatus.DOWN)
        override val status = mutableStatus
        override suspend fun up(config: WgConfig) = TunnelStatus.UP
        override suspend fun down() = Unit
        override fun permissionIntent(): Intent? = null
    }
}
