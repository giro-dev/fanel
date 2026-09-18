package dev.agiro.fanel.notifications.domain;

import com.interaso.webpush.VapidKeys;
import dev.agiro.fanel.notifications.infra.VapidKeyPairRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Holds the VAPID key pair used to sign Web Push requests. Keys can be pinned via
 * {@code fanel.vapid.public-key} / {@code fanel.vapid.private-key} (env vars); otherwise a pair is
 * generated on first use and persisted in {@link VapidKeyPair} so it survives restarts (subscriptions
 * collected from browsers are only valid for as long as the public key that produced them).
 */
@Component
public class VapidKeysProvider {
    private final VapidKeys vapidKeys;
    private final String applicationServerKeyBase64Url;

    public VapidKeysProvider(VapidKeyPairRepository repository,
                             @Value("${fanel.vapid.public-key:}") String configuredPublicKey,
                             @Value("${fanel.vapid.private-key:}") String configuredPrivateKey) {
        this.vapidKeys = resolve(repository, configuredPublicKey, configuredPrivateKey);
        this.applicationServerKeyBase64Url = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(vapidKeys.getApplicationServerKey());
    }

    private VapidKeys resolve(VapidKeyPairRepository repository, String configuredPublicKey, String configuredPrivateKey) {
        if (!configuredPublicKey.isBlank() && !configuredPrivateKey.isBlank()) {
            return VapidKeys.create(configuredPublicKey, configuredPrivateKey);
        }
        return repository.findFirstByOrderByCreatedAtAsc()
                .map(pair -> VapidKeys.create(pair.getX509PublicKey(), pair.getPkcs8PrivateKey()))
                .orElseGet(() -> {
                    VapidKeys generated = VapidKeys.generate();
                    repository.save(new VapidKeyPair(generated.getX509PublicKey(), generated.getPkcs8PrivateKey()));
                    return generated;
                });
    }

    public VapidKeys keys() { return vapidKeys; }

    /** The public key encoded as URL-safe Base64, as expected by `PushManager.subscribe`'s `applicationServerKey`. */
    public String applicationServerKeyBase64Url() { return applicationServerKeyBase64Url; }
}
