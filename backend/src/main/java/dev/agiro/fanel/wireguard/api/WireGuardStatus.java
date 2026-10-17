package dev.agiro.fanel.wireguard.api;

public record WireGuardStatus(boolean enabled, String interfaceName, String serverPublicKey,
                              boolean interfaceReachable, String endpoint, String network) { }
