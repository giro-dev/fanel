package dev.agiro.fanel.wireguard.infra;

import dev.agiro.fanel.wireguard.domain.WireGuardPairingToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WireGuardPairingTokenRepository extends JpaRepository<WireGuardPairingToken, UUID> {
    @Modifying
    @Query(value = "UPDATE wg_pairing_token SET used_at = :now WHERE token_hash = :hash AND used_at IS NULL AND expires_at > :now", nativeQuery = true)
    int consume(@Param("hash") String hash, @Param("now") Instant now);
    Optional<WireGuardPairingToken> findByTokenHash(String tokenHash);
}
