package dev.agiro.fanel.notifications.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interaso.webpush.WebPush;
import com.interaso.webpush.WebPushService;
import dev.agiro.fanel.notifications.infra.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Sends Web Push notifications, dropping subscriptions the push service reports as expired. */
@Component
public class PushSender {
    private static final Logger log = LoggerFactory.getLogger(PushSender.class);

    private final WebPushService webPushService;
    private final PushSubscriptionRepository subscriptions;
    private final ObjectMapper objectMapper;

    public PushSender(VapidKeysProvider vapidKeysProvider, PushSubscriptionRepository subscriptions,
                      @Value("${fanel.vapid.subject:mailto:admin@example.com}") String subject) {
        this.webPushService = new WebPushService(subject, vapidKeysProvider.keys());
        this.subscriptions = subscriptions;
        this.objectMapper = new ObjectMapper();
    }

    public void send(PushSubscription subscription, String title, String body) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body));
        } catch (Exception e) {
            log.warn("Could not serialize push payload", e);
            return;
        }
        try {
            WebPush.SubscriptionState state = webPushService.send(payload, subscription.getEndpoint(),
                    subscription.getP256dh(), subscription.getAuth(), null, null, null);
            if (state == WebPush.SubscriptionState.EXPIRED) {
                subscriptions.deleteById(subscription.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to send push notification to {}: {}", subscription.getEndpoint(), e.getMessage());
        }
    }
}
