package dev.agiro.fanel.notifications.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** A browser's Web Push subscription for a household member, used to deliver reminder notifications. */
@Entity
@Table(name = "push_subscription")
public class PushSubscription extends UuidEntity {
    @Column(name = "household_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;

    @Column(name = "member_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID memberId;

    @Column(nullable = false, unique = true, length = 2048)
    private String endpoint;

    @Column(nullable = false, length = 255)
    private String p256dh;

    @Column(nullable = false, length = 255)
    private String auth;

    private Instant createdAt;

    protected PushSubscription() {}

    public PushSubscription(UUID householdId, UUID memberId, String endpoint, String p256dh, String auth) {
        this.householdId = householdId;
        this.memberId = memberId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = Instant.now();
    }

    public UUID getHouseholdId() { return householdId; }
    public UUID getMemberId() { return memberId; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }
    public Instant getCreatedAt() { return createdAt; }
}
