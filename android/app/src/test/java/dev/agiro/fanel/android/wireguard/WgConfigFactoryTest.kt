package dev.agiro.fanel.android.wireguard

import com.wireguard.crypto.KeyPair
import com.wireguard.config.InetNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class WgConfigFactoryTest {
    @Test
    fun buildsSplitTunnelForTheFanelApplication() {
        val privateKey = KeyPair()
        val serverKey = KeyPair()
        val config = WgConfig(
            peerId = "peer-id",
            privateKey = privateKey.privateKey.toBase64(),
            publicKey = privateKey.publicKey.toBase64(),
            assignedAddress = "10.8.0.2/32",
            serverPublicKey = serverKey.publicKey.toBase64(),
            presharedKey = null,
            endpoint = "vpn.example.net:51820",
            allowedIps = listOf("10.8.0.0/24"),
            persistentKeepalive = 25,
            tunnelBaseUrl = "https://10.8.0.1:8443",
            lanBaseUrl = "https://fanel.home:8443",
            serverId = "server-id",
            spkiSha256 = Base64.getEncoder().encodeToString(ByteArray(32)),
        )

        val built = WgConfigFactory.build(config, "dev.agiro.fanel.android")
        val iface = built.getInterface()
        val peer = built.peers.single()

        assertEquals(setOf(InetNetwork.parse("10.8.0.2/32")), iface.addresses)
        assertEquals(1280, iface.mtu.get())
        assertEquals(setOf("dev.agiro.fanel.android"), iface.includedApplications)
        assertEquals(setOf(InetNetwork.parse("10.8.0.0/24")), peer.allowedIps)
        assertEquals(25, peer.persistentKeepalive.get())
    }

    @Test
    fun reresolvesOnlyForStalledTransmitTrafficWithAStaleHandshake() {
        val previous = TunnelStats(rxBytes = 10, txBytes = 10, lastHandshakeEpochMillis = 1_000)
        assertTrue(
            shouldReresolve(
                previous,
                TunnelStats(rxBytes = 10, txBytes = 11, lastHandshakeEpochMillis = 1_000),
                currentTimeMillis = 32_000,
            ),
        )
        org.junit.Assert.assertFalse(
            shouldReresolve(
                previous,
                TunnelStats(rxBytes = 11, txBytes = 11, lastHandshakeEpochMillis = 1_000),
                currentTimeMillis = 32_000,
            ),
        )
    }
}
