package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.api.PeerRuntime;
import dev.agiro.fanel.wireguard.api.WireGuardAdapter;

import java.util.List;

public class DisabledWireGuardAdapter implements WireGuardAdapter {
    private WireGuardUnavailableException unavailable() {
        return new WireGuardUnavailableException("WireGuard is disabled");
    }
    @Override public String serverPublicKey() { throw unavailable(); }
    @Override public void addPeer(String publicKey, String allowedIp, String presharedKey) { throw unavailable(); }
    @Override public void removePeer(String publicKey) { throw unavailable(); }
    @Override public List<PeerRuntime> listPeers() { throw unavailable(); }
}
