package dev.agiro.fanel.wireguard.infra;

import dev.agiro.fanel.wireguard.domain.WireGuardPeer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WireGuardPeerRepository extends JpaRepository<WireGuardPeer, UUID> {
    Optional<WireGuardPeer> findByPublicKey(String publicKey);
    Optional<WireGuardPeer> findByAssignedIpAndStatus(String assignedIp, String status);
    List<WireGuardPeer> findAllByOrderByCreatedAtDesc();
    List<WireGuardPeer> findAllByStatus(String status);
}
