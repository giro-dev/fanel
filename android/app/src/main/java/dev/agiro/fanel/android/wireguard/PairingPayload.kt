package dev.agiro.fanel.android.wireguard

import com.google.gson.JsonParser
import java.net.URI
import java.util.Base64

data class PairingPayload(
    val token: String,
    val serverId: String,
    val lanBaseUrl: String,
    val tunnelBaseUrl: String,
    val spkiSha256: String,
) {
    companion object {
        fun parse(link: String): PairingPayload {
            try {
                val uri = URI(link)
                require(uri.scheme == "fanel" && uri.host == "pair")
                val encoded = uri.rawQuery.orEmpty().split('&')
                    .map { it.split('=', limit = 2) }
                    .firstOrNull { it.firstOrNull() == "d" }
                    ?.getOrNull(1)
                    ?: throw InvalidPairingPayload()
                val json = JsonParser.parseString(
                    String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8),
                ).asJsonObject
                require(json.get("v")?.asInt == 1)
                val token = json.string("token")
                val serverId = json.string("serverId")
                val lanBaseUrl = json.string("lanBaseUrl")
                val tunnelBaseUrl = json.string("tunnelBaseUrl")
                val spkiSha256 = json.string("spkiSha256")
                require(httpsBaseUrl(lanBaseUrl))
                require(httpsBaseUrl(tunnelBaseUrl))
                require(Base64.getDecoder().decode(spkiSha256).size == 32)
                return PairingPayload(token, serverId, lanBaseUrl, tunnelBaseUrl, spkiSha256)
            } catch (exception: Exception) {
                if (exception is InvalidPairingPayload) throw exception
                throw InvalidPairingPayload()
            }
        }

        private fun com.google.gson.JsonObject.string(name: String): String =
            get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString?.takeIf { it.isNotBlank() } ?: throw InvalidPairingPayload()

        private fun httpsBaseUrl(value: String): Boolean {
            val uri = URI(value)
            return uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
        }
    }
}

class InvalidPairingPayload : IllegalArgumentException("Invalid pairing payload")
