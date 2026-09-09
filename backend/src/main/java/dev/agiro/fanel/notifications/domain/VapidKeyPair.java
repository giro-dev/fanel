package dev.agiro.fanel.notifications.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persisted VAPID key pair, generated once and reused across restarts so that push subscriptions
 * collected from browsers stay valid. Only one row is expected to ever exist; see {@link VapidKeysProvider}.
 */
@Entity
@Table(name = "vapid_key_pair")
public class VapidKeyPair extends UuidEntity {
    @Column(name = "x509_public_key", nullable = false, length = 512)
    private String x509PublicKey;

    @Column(name = "pkcs8_private_key", nullable = false, length = 512)
    private String pkcs8PrivateKey;

    private Instant createdAt;

    protected VapidKeyPair() {}

    public VapidKeyPair(String x509PublicKey, String pkcs8PrivateKey) {
        this.x509PublicKey = x509PublicKey;
        this.pkcs8PrivateKey = pkcs8PrivateKey;
        this.createdAt = Instant.now();
    }

    public String getX509PublicKey() { return x509PublicKey; }
    public String getPkcs8PrivateKey() { return pkcs8PrivateKey; }
    public Instant getCreatedAt() { return createdAt; }
}
