package dev.agiro.fanel.wireguard.api;

import java.time.Instant;

public record PairingTokenResponse(String payload, String qrPngBase64, Instant expiresAt) { }
