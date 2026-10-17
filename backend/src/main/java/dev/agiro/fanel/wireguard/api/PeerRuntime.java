package dev.agiro.fanel.wireguard.api;

import java.time.Instant;
import java.util.List;

public record PeerRuntime(String publicKey, String endpoint, List<String> allowedIps,
                          Instant latestHandshake, long rxBytes, long txBytes,
                          Integer persistentKeepalive) { }
