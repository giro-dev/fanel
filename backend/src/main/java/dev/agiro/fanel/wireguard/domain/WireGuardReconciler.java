package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.api.PeerRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WireGuardReconciler {
    private static final Logger log = LoggerFactory.getLogger(WireGuardReconciler.class);
    private final WireGuardPeerService service;

    public WireGuardReconciler(WireGuardPeerService service) { this.service = service; }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { reconcile(); }

    @Scheduled(fixedDelayString = "${fanel.wireguard.reconcile-interval:PT5M}")
    public void scheduledReconcile() { reconcile(); }

    public void reconcile() {
        if (!service.properties().isEnabled()) return;
        try {
            service.refreshActiveAddresses();
            Map<String, PeerRuntime> runtime = service.adapter().listPeers().stream()
                    .collect(Collectors.toMap(PeerRuntime::publicKey, Function.identity(), (first, ignored) -> first));
            Map<String, WireGuardPeer> database = service.allPeers().stream()
                    .collect(Collectors.toMap(WireGuardPeer::getPublicKey, Function.identity(), (first, ignored) -> first));
            for (WireGuardPeer peer : database.values()) {
                PeerRuntime current = runtime.get(peer.getPublicKey());
                if ("ACTIVE".equals(peer.getStatus())) {
                    if (current == null) {
                        service.adapter().addPeer(peer.getPublicKey(), peer.getAssignedIp() + "/32", service.decryptPsk(peer));
                    } else if (current.latestHandshake() != null) {
                        service.updateHandshake(peer.getPublicKey(), current.latestHandshake());
                    }
                } else if (current != null) {
                    service.adapter().removePeer(peer.getPublicKey());
                }
            }
            Set<String> known = database.keySet();
            for (PeerRuntime current : runtime.values()) {
                if (!known.contains(current.publicKey())) {
                    log.warn("WireGuard runtime peer is not present in the database: {}", current.publicKey());
                    if (service.properties().isRemoveUnknownPeers()) service.adapter().removePeer(current.publicKey());
                }
            }
            service.refreshActiveAddresses();
        } catch (Exception exception) {
            log.error("WireGuard reconciliation failed; the application will continue and retry later", exception);
        }
    }
}
