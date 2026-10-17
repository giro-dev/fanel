package dev.agiro.fanel.wireguard.api;

import java.util.List;

public interface WireGuardAdapter {
    String serverPublicKey();
    void addPeer(String publicKey, String allowedIp, String presharedKey);
    void removePeer(String publicKey);
    List<PeerRuntime> listPeers();
}
