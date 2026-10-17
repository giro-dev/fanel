package dev.agiro.fanel.wireguard.api;

public record EnrollResponse(String peerId, String assignedAddress, String serverPublicKey,
                             String presharedKey, String endpoint, String allowedIps,
                             Integer persistentKeepalive, String tunnelBaseUrl, String lanBaseUrl) { }
