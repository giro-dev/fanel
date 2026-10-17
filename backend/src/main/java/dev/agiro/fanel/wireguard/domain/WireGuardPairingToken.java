package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wg_pairing_token")
public class WireGuardPairingToken extends UuidEntity {
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "created_by", length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WireGuardPairingToken() { }
    public WireGuardPairingToken(String tokenHash, Instant expiresAt, UUID createdBy, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
