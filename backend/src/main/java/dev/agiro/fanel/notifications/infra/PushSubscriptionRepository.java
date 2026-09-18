package dev.agiro.fanel.notifications.infra;

import dev.agiro.fanel.notifications.domain.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    List<PushSubscription> findByHouseholdIdAndMemberId(UUID householdId, UUID memberId);

    void deleteByEndpoint(String endpoint);

    void deleteByHouseholdIdAndEndpoint(UUID householdId, String endpoint);
}
