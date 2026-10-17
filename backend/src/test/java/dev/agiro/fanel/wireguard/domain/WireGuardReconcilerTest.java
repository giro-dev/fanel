package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.api.PeerRuntime;
import dev.agiro.fanel.wireguard.api.WireGuardAdapter;
import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;

class WireGuardReconcilerTest {
    @Test
    void restoresMissingActivePeersAndRemovesRevokedPeers() {
        WireGuardPeer active = new WireGuardPeer("phone", key(1), null, "10.8.0.2", null, Instant.EPOCH);
        WireGuardPeer revoked = new WireGuardPeer("tablet", key(2), null, "10.8.0.3", null, Instant.EPOCH);
        WireGuardPeer connected = new WireGuardPeer("laptop", key(3), null, "10.8.0.4", null, Instant.EPOCH);
        revoked.revoke(Instant.EPOCH);
        WireGuardPeerService service = mock(WireGuardPeerService.class);
        WireGuardAdapter adapter = mock(WireGuardAdapter.class);
        WireGuardProperties properties = enabledProperties();
        when(service.properties()).thenReturn(properties);
        when(service.adapter()).thenReturn(adapter);
        when(service.allPeers()).thenReturn(List.of(active, revoked, connected));
        when(service.decryptPsk(active)).thenReturn(null);
        Instant handshake = Instant.parse("2026-01-01T00:00:00Z");
        when(adapter.listPeers()).thenReturn(List.of(runtime(key(2), "10.8.0.3/32"),
                runtime(key(3), "10.8.0.4/32", handshake)));

        new WireGuardReconciler(service).reconcile();

        verify(adapter).addPeer(key(1), "10.8.0.2/32", null);
        verify(adapter).removePeer(key(2));
        verify(service).updateHandshake(key(3), handshake);
        verify(service, org.mockito.Mockito.times(2)).refreshActiveAddresses();
    }

    @Test
    void warnsAndPreservesUnknownPeersUnlessConfiguredToRemoveThem() {
        WireGuardPeerService service = mock(WireGuardPeerService.class);
        WireGuardAdapter adapter = mock(WireGuardAdapter.class);
        WireGuardProperties properties = enabledProperties();
        when(service.properties()).thenReturn(properties);
        when(service.adapter()).thenReturn(adapter);
        when(service.allPeers()).thenReturn(List.of());
        when(adapter.listPeers()).thenReturn(List.of(runtime(key(9), "10.8.0.9/32")));
        WireGuardReconciler reconciler = new WireGuardReconciler(service);

        reconciler.reconcile();
        verify(adapter, never()).removePeer(key(9));
        properties.setRemoveUnknownPeers(true);
        reconciler.reconcile();
        verify(adapter).removePeer(key(9));
    }

    @Test
    void catchesRuntimeAdapterFailures() {
        WireGuardPeerService service = mock(WireGuardPeerService.class);
        WireGuardAdapter adapter = mock(WireGuardAdapter.class);
        when(service.properties()).thenReturn(enabledProperties());
        when(service.adapter()).thenReturn(adapter);
        when(adapter.listPeers()).thenThrow(new IllegalStateException("wg0 missing"));
        new WireGuardReconciler(service).reconcile();
    }

    private static WireGuardProperties enabledProperties() {
        WireGuardProperties properties = new WireGuardProperties();
        properties.setEnabled(true);
        return properties;
    }

    private static PeerRuntime runtime(String key, String ip) {
        return runtime(key, ip, null);
    }

    private static PeerRuntime runtime(String key, String ip, Instant handshake) {
        return new PeerRuntime(key, null, List.of(ip), handshake, 0, 0, null);
    }

    private static String key(int lastByte) {
        byte[] bytes = new byte[32];
        bytes[31] = (byte) lastByte;
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }
}
