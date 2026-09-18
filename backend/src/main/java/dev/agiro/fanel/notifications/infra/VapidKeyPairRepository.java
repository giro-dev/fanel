package dev.agiro.fanel.notifications.infra;

import dev.agiro.fanel.notifications.domain.VapidKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VapidKeyPairRepository extends JpaRepository<VapidKeyPair, UUID> {
    Optional<VapidKeyPair> findFirstByOrderByCreatedAtAsc();
}
