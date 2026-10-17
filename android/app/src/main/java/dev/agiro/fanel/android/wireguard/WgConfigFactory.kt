package dev.agiro.fanel.android.wireguard

import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key

object WgConfigFactory {
    fun build(config: WgConfig, packageName: String): Config {
        val iface = Interface.Builder()
            .addAddress(InetNetwork.parse(config.assignedAddress))
            .setKeyPair(com.wireguard.crypto.KeyPair(Key.fromBase64(config.privateKey)))
            .includeApplication(packageName)
            .setMtu(1280)
            .build()
        val peer = Peer.Builder()
            .setPublicKey(Key.fromBase64(config.serverPublicKey))
            .setEndpoint(InetEndpoint.parse(config.endpoint))
            .addAllowedIps(config.allowedIps.map(InetNetwork::parse))
            .setPersistentKeepalive(config.persistentKeepalive)
            .apply { config.presharedKey?.let { setPreSharedKey(Key.fromBase64(it)) } }
            .build()
        return Config.Builder().setInterface(iface).addPeer(peer).build()
    }
}
