package dev.agiro.fanel.notifications.domain;

import dev.agiro.fanel.notifications.infra.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PushSubscriptionsService {
    private final PushSubscriptionRepository subscriptions;

    public PushSubscriptionsService(PushSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    /** Registers (or re-registers, if the browser already had a subscription with this endpoint) a push subscription. */
    public void subscribe(UUID householdId, UUID memberId, String endpoint, String p256dh, String auth) {
        subscriptions.deleteByEndpoint(endpoint);
        subscriptions.save(new PushSubscription(householdId, memberId, endpoint, p256dh, auth));
    }

    public void unsubscribe(UUID householdId, String endpoint) {
        subscriptions.deleteByHouseholdIdAndEndpoint(householdId, endpoint);
    }
}
