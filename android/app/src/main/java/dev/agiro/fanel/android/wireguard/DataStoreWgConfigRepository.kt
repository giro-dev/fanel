package dev.agiro.fanel.android.wireguard

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.agiro.fanel.android.KeystoreCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DataStoreWgConfigRepository(
    context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher("fanel_wg"),
) : WgConfigRepository {
    private val store = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile("fanel_wg") },
    )

    @Volatile
    private var cached: WgConfig? = runBlocking(Dispatchers.IO) {
        store.data.first().toConfig(cipher)
    }

    override val current: WgConfig?
        get() = cached

    override suspend fun save(c: WgConfig) {
        store.edit { prefs ->
            prefs[PEER_ID] = c.peerId
            prefs[PRIVATE_KEY] = cipher.encrypt(c.privateKey)
            prefs[PUBLIC_KEY] = c.publicKey
            prefs[ASSIGNED_ADDRESS] = c.assignedAddress
            prefs[SERVER_PUBLIC_KEY] = c.serverPublicKey
            c.presharedKey?.let { prefs[PRESHARED_KEY] = cipher.encrypt(it) } ?: prefs.remove(PRESHARED_KEY)
            prefs[ENDPOINT] = c.endpoint
            prefs[ALLOWED_IPS] = c.allowedIps.toSet()
            prefs[KEEPALIVE] = c.persistentKeepalive.toString()
            prefs[TUNNEL_URL] = c.tunnelBaseUrl
            prefs[LAN_URL] = c.lanBaseUrl
            prefs[SERVER_ID] = c.serverId
            prefs[SPKI] = c.spkiSha256
        }
        cached = c
    }

    override suspend fun clear() {
        store.edit { it.clear() }
        cached = null
    }

    private fun androidx.datastore.preferences.core.Preferences.toConfig(cipher: KeystoreCipher): WgConfig? {
        val privateKey = this[PRIVATE_KEY] ?: return null
        return WgConfig(
            peerId = this[PEER_ID] ?: return null,
            privateKey = cipher.decrypt(privateKey),
            publicKey = this[PUBLIC_KEY] ?: return null,
            assignedAddress = this[ASSIGNED_ADDRESS] ?: return null,
            serverPublicKey = this[SERVER_PUBLIC_KEY] ?: return null,
            presharedKey = this[PRESHARED_KEY]?.let(cipher::decrypt),
            endpoint = this[ENDPOINT] ?: return null,
            allowedIps = this[ALLOWED_IPS]?.toList().orEmpty(),
            persistentKeepalive = this[KEEPALIVE]?.toIntOrNull() ?: return null,
            tunnelBaseUrl = this[TUNNEL_URL] ?: return null,
            lanBaseUrl = this[LAN_URL] ?: return null,
            serverId = this[SERVER_ID] ?: return null,
            spkiSha256 = this[SPKI] ?: return null,
        )
    }

    private companion object {
        val PEER_ID = stringPreferencesKey("peer_id")
        val PRIVATE_KEY = stringPreferencesKey("private_key")
        val PUBLIC_KEY = stringPreferencesKey("public_key")
        val ASSIGNED_ADDRESS = stringPreferencesKey("assigned_address")
        val SERVER_PUBLIC_KEY = stringPreferencesKey("server_public_key")
        val PRESHARED_KEY = stringPreferencesKey("preshared_key")
        val ENDPOINT = stringPreferencesKey("endpoint")
        val ALLOWED_IPS = stringSetPreferencesKey("allowed_ips")
        val KEEPALIVE = stringPreferencesKey("persistent_keepalive")
        val TUNNEL_URL = stringPreferencesKey("tunnel_base_url")
        val LAN_URL = stringPreferencesKey("lan_base_url")
        val SERVER_ID = stringPreferencesKey("server_id")
        val SPKI = stringPreferencesKey("spki_sha256")
    }
}
