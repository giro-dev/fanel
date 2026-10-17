package dev.agiro.fanel.android.wireguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class PairingPayloadTest {
    private val pin = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    @Test
    fun parsesPaddedAndUnpaddedLinks() {
        val json = """{"v":1,"token":"token-123","serverId":"server-1","lanBaseUrl":"https://fanel.home:8443","tunnelBaseUrl":"https://10.8.0.1:8443","spkiSha256":"$pin"}"""

        assertEquals(expected(), PairingPayload.parse(link(json, padded = true)))
        assertEquals(expected(), PairingPayload.parse(link(json, padded = false)))
    }

    @Test
    fun rejectsUnsupportedOrMalformedPayloads() {
        val valid = """{"v":1,"token":"token-123","serverId":"server-1","lanBaseUrl":"https://fanel.home","tunnelBaseUrl":"https://10.8.0.1","spkiSha256":"$pin"}"""
        val cases = listOf(
            link(valid, scheme = "https"),
            "fanel://pair?d=%%%not-base64%%%",
            link(valid.replace("\"v\":1", "\"v\":2")),
            link(valid.replace("\"token\":\"token-123\",", "")),
            link(valid.replace(pin, Base64.getEncoder().encodeToString(ByteArray(31)))),
        )

        cases.forEach { link -> assertThrows(InvalidPairingPayload::class.java) { PairingPayload.parse(link) } }
    }

    private fun expected() = PairingPayload(
        token = "token-123",
        serverId = "server-1",
        lanBaseUrl = "https://fanel.home:8443",
        tunnelBaseUrl = "https://10.8.0.1:8443",
        spkiSha256 = pin,
    )

    private fun link(json: String, padded: Boolean = false, scheme: String = "fanel"): String {
        val encoder = if (padded) Base64.getUrlEncoder() else Base64.getUrlEncoder().withoutPadding()
        return "$scheme://pair?d=${encoder.encodeToString(json.toByteArray())}"
    }
}
