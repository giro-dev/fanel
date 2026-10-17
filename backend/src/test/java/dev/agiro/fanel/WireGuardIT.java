package dev.agiro.fanel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.fanel.wireguard.api.PeerRuntime;
import dev.agiro.fanel.wireguard.api.WireGuardAdapter;
import dev.agiro.fanel.wireguard.domain.ActivePeerAddresses;
import dev.agiro.fanel.wireguard.domain.WireGuardPairingToken;
import dev.agiro.fanel.wireguard.domain.WireGuardPeer;
import dev.agiro.fanel.wireguard.infra.WireGuardPairingTokenRepository;
import dev.agiro.fanel.wireguard.infra.WireGuardPeerRepository;
import dev.agiro.fanel.wireguard.domain.WireGuardPeerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Import(WireGuardIT.WireGuardTestConfiguration.class)
@SpringBootTest
abstract class WireGuardIT {
    private static final String ADMIN = "/api/v1/admin/wg";
    private static final String PUBLIC_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String ENROLL = "/api/v1/wg/enroll";

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired WireGuardPeerRepository peers;
    @Autowired WireGuardPairingTokenRepository tokens;
    @Autowired InMemoryWireGuardAdapter adapter;
    @Autowired MutableClock clock;
    @Autowired WireGuardPeerService peerService;

    @DynamicPropertySource
    static void wireGuardProperties(DynamicPropertyRegistry registry) {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        registry.add("fanel.wireguard.enabled", () -> "true");
        registry.add("fanel.wireguard.interface-name", () -> "wg0");
        registry.add("fanel.wireguard.network", () -> "10.8.0.0/24");
        registry.add("fanel.wireguard.server-address", () -> "10.8.0.1");
        registry.add("fanel.wireguard.endpoint", () -> "vpn.example.test:51820");
        registry.add("fanel.wireguard.lan-base-url", () -> "http://192.168.1.20:8080");
        registry.add("fanel.wireguard.tunnel-base-url", () -> "https://10.8.0.1:8443");
        registry.add("fanel.wireguard.psk-encryption-key", () -> key);
        registry.add("fanel.wireguard.tls-spki-sha256", () -> key);
        registry.add("fanel.wireguard.server-id", () -> "d3d27df1-5134-4ad5-8fca-05e62d4551e6");
        registry.add("fanel.wireguard.command-timeout", () -> "1s");
        registry.add("fanel.wireguard.pairing-token-ttl", () -> "10m");
        registry.add("fanel.wireguard.trusted-proxies", () -> "");
    }

    @BeforeEach
    void clearDatabaseAndRuntime() {
        peers.deleteAll();
        tokens.deleteAll();
        adapter.clear();
        clock.set(Instant.parse("2026-01-01T00:00:00Z"));
        peerService.refreshActiveAddresses();
    }

    @Test
    void pairsAndEnrollsPeerAndRejectsTokenReuse() throws Exception {
        String token = createToken();
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.11"))
                        .content(enrollBody(token, PUBLIC_KEY, "  Albert phone  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAddress").value("10.8.0.2/32"))
                .andExpect(jsonPath("$.serverPublicKey").value(PUBLIC_KEY))
                .andExpect(jsonPath("$.presharedKey").isNotEmpty())
                .andExpect(jsonPath("$.endpoint").value("vpn.example.test:51820"))
                .andExpect(jsonPath("$.allowedIps").value("10.8.0.0/24"))
                .andExpect(jsonPath("$.persistentKeepalive").value(25));
        assertThat(adapter.snapshot()).containsKey(PUBLIC_KEY);
        assertThat(adapter.snapshot().get(PUBLIC_KEY).presharedKey()).isNotBlank();
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.11"))
                        .content(enrollBody(token, key(2), "second")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenAndInvalidOrDuplicateKeysAreRejected() throws Exception {
        String token = createToken();
        clock.advance(Duration.ofMinutes(11));
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.12"))
                        .content(enrollBody(token, key(1), "expired")))
                .andExpect(status().isUnauthorized());

        clock.set(Instant.parse("2026-01-01T00:00:00Z"));
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.12"))
                        .content(enrollBody(createToken(), "bad", "invalid")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.12"))
                        .content(enrollBody(createToken(), PUBLIC_KEY, "first")))
                .andExpect(status().isOk());
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.12"))
                        .content(enrollBody(createToken(), PUBLIC_KEY, "duplicate")))
                .andExpect(status().isConflict());
    }

    @Test
    void marksPeerRevokedWhenRuntimeAdditionFails() throws Exception {
        adapter.setFailAdd(true);
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.13"))
                        .content(enrollBody(createToken(), PUBLIC_KEY, "failure")))
                .andExpect(status().isInternalServerError());
        assertThat(peers.findAll()).singleElement().extracting(WireGuardPeer::getStatus).isEqualTo("REVOKED");
    }

    @Test
    void doesNotConsumeTokenWhenServerPublicKeyIsUnavailable() throws Exception {
        String token = createToken();
        adapter.setServerKeyUnavailable(true);
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.14"))
                        .content(enrollBody(token, PUBLIC_KEY, "phone")))
                .andExpect(status().isServiceUnavailable());
        String tokenHash = java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        assertThat(tokens.findByTokenHash(tokenHash).orElseThrow().getUsedAt()).isNull();
    }

    @Test
    void allocatesDistinctAddressesUnderConcurrentEnrollments() throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(10);
        try {
            List<java.util.concurrent.Future<String>> results = java.util.stream.IntStream.range(0, 10)
                    .mapToObj(i -> executor.submit(() -> {
                        String token = createToken();
                        String response = mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                                        .with(remoteAddress("192.0.2." + (i + 1)))
                                        .content(enrollBody(token, key(i + 1), "device-" + i)))
                                .andReturn().getResponse().getContentAsString();
                        return mapper.readTree(response).path("assignedAddress").asText();
                    })).toList();
            List<String> addresses = results.stream().map(future -> {
                try { return future.get(); } catch (Exception exception) { throw new RuntimeException(exception); }
            }).toList();
            assertThat(addresses).hasSize(10).doesNotHaveDuplicates();
            assertThat(addresses).contains("10.8.0.2/32", "10.8.0.11/32");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void exposesIdentityAndAdminEndpointsAndRevokesPeer() throws Exception {
        mvc.perform(get("/api/v1/health/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverId").value("d3d27df1-5134-4ad5-8fca-05e62d4551e6"));
        mvc.perform(get(ADMIN + "/status"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(ADMIN + "/status").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.interfaceReachable").value(true));
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.15"))
                        .content(enrollBody(createToken(), PUBLIC_KEY, "phone")))
                .andExpect(status().isOk());
        WireGuardPeer peer = peers.findAll().getFirst();
        mvc.perform(delete(ADMIN + "/peers/" + peer.getId()).with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());
        assertThat(peers.findById(peer.getId()).orElseThrow().getStatus()).isEqualTo("REVOKED");
        assertThat(adapter.snapshot()).doesNotContainKey(PUBLIC_KEY);
        mvc.perform(delete(ADMIN + "/peers/" + peer.getId()).with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void pairingPayloadPreservesTheMobileContractOrder() throws Exception {
        String response = mvc.perform(post(ADMIN + "/pairing-tokens").with(httpBasic("admin", "admin")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String payload = mapper.readTree(response).path("payload").asText();
        JsonNode decoded = mapper.readTree(Base64.getUrlDecoder()
                .decode(payload.substring(payload.indexOf("d=") + 2)));
        java.util.List<String> names = new java.util.ArrayList<>();
        decoded.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactly("v", "token", "serverId",
                "lanBaseUrl", "tunnelBaseUrl", "spkiSha256");
    }

    @Test
    void peersMeAndTunnelPeerFilterUseRemoteAddress() throws Exception {
        mvc.perform(post(ENROLL).contentType(MediaType.APPLICATION_JSON)
                        .with(remoteAddress("198.51.100.16"))
                        .content(enrollBody(createToken(), PUBLIC_KEY, "phone")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/health/identity")
                        .with(remoteAddress("10.8.0.99")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/health/identity")
                        .with(remoteAddress("10.8.0.2")))
                .andExpect(status().isOk());
        mvc.perform(get(ADMIN + "/status")
                        .with(remoteAddress("10.8.0.2"))
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/wg/peers/me").with(httpBasic("admin", "admin"))
                        .with(remoteAddress("10.8.0.2")))
                .andExpect(status().isNoContent());
        assertThat(peers.findAll()).singleElement().extracting(WireGuardPeer::getStatus).isEqualTo("REVOKED");
    }

    @Test
    void appliesEnrollRateLimitWithRetryAfter() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post(ENROLL).with(remoteAddress("198.51.100.9"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(enrollBody("unknown", key(i), "rate")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post(ENROLL).with(remoteAddress("198.51.100.9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enrollBody("unknown", key(9), "rate")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    private String createToken() throws Exception {
        String response = mvc.perform(post(ADMIN + "/pairing-tokens").with(httpBasic("admin", "admin")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(response);
        String payload = json.path("payload").asText();
        String encoded = payload.substring(payload.indexOf("d=") + 2);
        return mapper.readTree(Base64.getUrlDecoder().decode(encoded)).path("token").asText();
    }

    private static String enrollBody(String token, String key, String deviceName) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "token", token, "publicKey", key, "deviceName", deviceName));
    }

    private static String key(int suffix) {
        byte[] bytes = new byte[32];
        bytes[31] = (byte) suffix;
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> { request.setRemoteAddr(address); return request; };
    }

    @TestConfiguration
    static class WireGuardTestConfiguration {
        @Bean @Primary InMemoryWireGuardAdapter fakeAdapter() { return new InMemoryWireGuardAdapter(); }
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
    }

    static class InMemoryWireGuardAdapter implements WireGuardAdapter {
        private final Map<String, RuntimePeer> peers = new ConcurrentHashMap<>();
        private volatile boolean failAdd;
        private volatile boolean serverKeyUnavailable;
        @Override public String serverPublicKey() { return serverKeyUnavailable ? null : PUBLIC_KEY; }
        @Override public void addPeer(String publicKey, String allowedIp, String presharedKey) {
            if (failAdd) throw new IllegalStateException("fake add failure");
            peers.put(publicKey, new RuntimePeer(publicKey, allowedIp, presharedKey));
        }
        @Override public void removePeer(String publicKey) { peers.remove(publicKey); }
        @Override public List<PeerRuntime> listPeers() {
            return peers.values().stream().map(peer -> new PeerRuntime(peer.publicKey(), null,
                    List.of(peer.allowedIp()), null, 0, 0, null)).toList();
        }
        Map<String, RuntimePeer> snapshot() { return Map.copyOf(peers); }
        void clear() { peers.clear(); failAdd = false; serverKeyUnavailable = false; }
        void setFailAdd(boolean value) { failAdd = value; }
        void setServerKeyUnavailable(boolean value) { serverKeyUnavailable = value; }
    }
    record RuntimePeer(String publicKey, String allowedIp, String presharedKey) { }

    static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void set(Instant value) { now = value; }
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
