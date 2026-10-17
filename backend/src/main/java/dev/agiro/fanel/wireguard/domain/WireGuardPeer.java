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
@Table(name = "wg_peer")
public class WireGuardPeer extends UuidEntity {
    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;
    @Column(name = "public_key", nullable = false, unique = true, length = 44)
    private String publicKey;
    @Column(name = "preshared_key", length = 255)
    private String presharedKey;
    @Column(name = "assigned_ip", nullable = false, unique = true, length = 15)
    private String assignedIp;
    @Column(nullable = false, length = 10)
    private String status;
    @Column(name = "owner_member_id", length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID ownerMemberId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "last_handshake_at")
    private Instant lastHandshakeAt;

    protected WireGuardPeer() { }

    public WireGuardPeer(String deviceName, String publicKey, String presharedKey,
                         String assignedIp, UUID ownerMemberId, Instant createdAt) {
        this.deviceName = deviceName;
        this.publicKey = publicKey;
        this.presharedKey = presharedKey;
        this.assignedIp = assignedIp;
        this.ownerMemberId = ownerMemberId;
        this.createdAt = createdAt;
        this.status = "ACTIVE";
    }

    public void revoke(Instant at) {
        status = "REVOKED";
        if (revokedAt == null) revokedAt = at;
    }
    public void handshake(Instant at) { lastHandshakeAt = at; }
    public String getDeviceName() { return deviceName; }
    public String getPublicKey() { return publicKey; }
    public String getPresharedKey() { return presharedKey; }
    public String getAssignedIp() { return assignedIp; }
    public String getStatus() { return status; }
    public UUID getOwnerMemberId() { return ownerMemberId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getLastHandshakeAt() { return lastHandshakeAt; }
}
