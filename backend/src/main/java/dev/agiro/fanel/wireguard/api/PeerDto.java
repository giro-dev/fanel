package dev.agiro.fanel.wireguard.api;

import java.time.Instant;
import java.util.UUID;

public record PeerDto(UUID id, String deviceName, String publicKey, String assignedIp, String status,
                      UUID ownerMemberId, Instant createdAt, Instant revokedAt, Instant lastHandshakeAt) { }
