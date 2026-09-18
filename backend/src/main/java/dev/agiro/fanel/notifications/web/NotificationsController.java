package dev.agiro.fanel.notifications.web;

import dev.agiro.fanel.notifications.domain.PushSubscriptionsService;
import dev.agiro.fanel.notifications.domain.VapidKeysProvider;
import dev.agiro.fanel.shared.security.CurrentAccess;
import dev.agiro.fanel.shared.web.ForbiddenException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class NotificationsController {
    private final VapidKeysProvider vapidKeysProvider;
    private final PushSubscriptionsService subscriptionsService;
    private final CurrentAccess access;

    public NotificationsController(VapidKeysProvider vapidKeysProvider, PushSubscriptionsService subscriptionsService,
                                   CurrentAccess access) {
        this.vapidKeysProvider = vapidKeysProvider;
        this.subscriptionsService = subscriptionsService;
        this.access = access;
    }

    @GetMapping("/api/v1/notifications/vapid-public-key")
    public Map<String, String> vapidPublicKey() {
        return Map.of("publicKey", vapidKeysProvider.applicationServerKeyBase64Url());
    }

    @PostMapping("/api/v1/households/{householdId}/notifications/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribe(@PathVariable UUID householdId, @Valid @RequestBody SubscribeRequest request) {
        UUID memberId = currentMemberId(householdId);
        subscriptionsService.subscribe(householdId, memberId, request.endpoint(), request.keys().p256dh(), request.keys().auth());
    }

    @DeleteMapping("/api/v1/households/{householdId}/notifications/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable UUID householdId, @Valid @RequestBody UnsubscribeRequest request) {
        requireMemberOf(householdId);
        subscriptionsService.unsubscribe(householdId, request.endpoint());
    }

    private UUID currentMemberId(UUID householdId) {
        requireMemberOf(householdId);
        return access.memberId().orElseThrow(() ->
                new ForbiddenException("Only a household member's own account can register for push notifications"));
    }

    private void requireMemberOf(UUID householdId) {
        UUID currentHousehold = access.householdId().orElseThrow(() ->
                new ForbiddenException("Only an authenticated household member can manage push notifications"));
        if (!currentHousehold.equals(householdId)) {
            throw new ForbiddenException("Member does not belong to this household");
        }
    }

    public record SubscribeRequest(@NotBlank String endpoint, @Valid PushKeys keys) {}
    public record PushKeys(@NotBlank String p256dh, @NotBlank String auth) {}
    public record UnsubscribeRequest(@NotBlank String endpoint) {}
}
