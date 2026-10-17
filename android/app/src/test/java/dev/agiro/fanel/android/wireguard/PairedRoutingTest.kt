package dev.agiro.fanel.android.wireguard

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.Base64

class PairedRoutingTest {
    @get:Rule
    val lanServer = MockWebServer()
    @get:Rule
    val remoteServer = MockWebServer()

    @Test
    fun rewritesLanAndRemoteAndAddsAuthorizationAfterRewrite() {
        lanServer.enqueue(MockResponse().setResponseCode(200))
        remoteServer.enqueue(MockResponse().setResponseCode(200))
        val config = config()
        val state = ConnectionStateHolder()
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(state) { config })
            .addInterceptor {
                it.proceed(it.request().newBuilder().header("Authorization", "Bearer test-token").build())
            }
            .build()

        listOf(
            ConnectionMode.LAN to (lanServer to lanServer.url("/")),
            ConnectionMode.REMOTE to (remoteServer to remoteServer.url("/")),
        ).forEach { (mode, target) ->
            state.set(ConnectionState(mode))
            client.newCall(Request.Builder().url(LOGICAL_BASE_URL + "api/check").build()).execute().use {
                assertEquals(200, it.code)
            }
            val (targetServer, base) = target
            val request = targetServer.takeRequest()
            assertEquals("/api/check", request.path)
            assertEquals("Bearer test-token", request.getHeader("Authorization"))
            assertEquals(base.host, request.requestUrl?.host)
        }
    }

    @Test
    fun unreachableModesFailBeforeSendingARequest() {
        val config = config()
        val state = ConnectionStateHolder()
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(state) { config })
            .addInterceptor {
                it.proceed(it.request().newBuilder().header("Authorization", "Bearer token").build())
            }
            .build()

        listOf(ConnectionMode.UNKNOWN, ConnectionMode.OFFLINE, ConnectionMode.ERROR).forEach { mode ->
            state.set(ConnectionState(mode))
            assertThrows(IOException::class.java) {
                client.newCall(Request.Builder().url(LOGICAL_BASE_URL + "api/check").build()).execute()
            }
        }
        assertEquals(0, lanServer.requestCount)
        assertEquals(0, remoteServer.requestCount)
    }

    private fun config(): WgConfig {
        val key = com.wireguard.crypto.KeyPair()
        return WgConfig(
            peerId = "peer-1",
            privateKey = key.privateKey.toBase64(),
            publicKey = key.publicKey.toBase64(),
            assignedAddress = "10.8.0.2/32",
            serverPublicKey = key.publicKey.toBase64(),
            presharedKey = null,
            endpoint = "vpn.example.net:51820",
            allowedIps = listOf("10.8.0.0/24"),
            persistentKeepalive = 25,
            tunnelBaseUrl = remoteServer.url("/").toString(),
            lanBaseUrl = lanServer.url("/").toString(),
            serverId = "server-id",
            spkiSha256 = Base64.getEncoder().encodeToString(ByteArray(32)),
        )
    }
}
