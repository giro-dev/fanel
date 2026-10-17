package dev.agiro.fanel.android.wireguard

data class WgConfig(
    val peerId: String,
    val privateKey: String,
    val publicKey: String,
    val assignedAddress: String,
    val serverPublicKey: String,
    val presharedKey: String?,
    val endpoint: String,
    val allowedIps: List<String>,
    val persistentKeepalive: Int,
    val tunnelBaseUrl: String,
    val lanBaseUrl: String,
    val serverId: String,
    val spkiSha256: String,
)

interface WgConfigRepository {
    val current: WgConfig?
    suspend fun save(c: WgConfig)
    suspend fun clear()
}
