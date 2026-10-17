package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.api.PeerRuntime;
import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class WireGuardCoreTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void validatesKeysAndCidr() {
        WgKeys.validatePublicKey(KEY);
        assertThatThrownBy(() -> WgKeys.validatePublicKey("a".repeat(43) + "=="))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WgKeys.validatePublicKey("!".repeat(43) + "="))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WgKeys.validatePublicKey("a".repeat(43)))
                .isInstanceOf(IllegalArgumentException.class);
        Ipv4Cidr network = Ipv4Cidr.parse("10.8.0.0/24");
        assertThat(network.contains("10.8.0.1")).isTrue();
        assertThat(network.contains("10.8.1.1")).isFalse();
        assertThat(network.hosts()).contains("10.8.0.1", "10.8.0.254");
        assertThatThrownBy(() -> Ipv4Cidr.parse("10.8.0.0/33"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allocatesOnlyAvailableHostAddresses() {
        Ipv4Cidr network = Ipv4Cidr.parse("10.8.0.0/29");
        assertThat(WireGuardAddressAllocator.firstAvailable(network, "10.8.0.2", List.of()))
                .contains("10.8.0.1");
        assertThat(WireGuardAddressAllocator.firstAvailable(network, "10.8.0.2",
                List.of("10.8.0.1", "10.8.0.3"))).contains("10.8.0.4");
        assertThat(WireGuardAddressAllocator.firstAvailable(Ipv4Cidr.parse("10.8.0.0/30"),
                "10.8.0.1", List.of("10.8.0.2"))).isEmpty();
    }

    @Test
    void validatesEnabledPropertiesTogetherAndIgnoresDisabledRequirements() {
        WireGuardProperties properties = new WireGuardProperties();
        properties.validateEnabledConfiguration();
        properties.setEnabled(true);
        assertThatThrownBy(properties::validateEnabledConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("endpoint is required")
                .hasMessageContaining("lan-base-url is required")
                .hasMessageContaining("server-id must be a UUID")
                .hasMessageContaining("tls-spki-sha256 must be base64 encoding of 32 bytes")
                .hasMessageContaining("psk-encryption-key must be base64 encoding of 32 bytes");
    }

    @Test
    void recognizesPrivateAndLocalIpRangesWithoutDnsLookup() {
        WireGuardProperties properties = new WireGuardProperties();
        assertThat(properties.isPrivateOnlyAddress("192.168.1.5")).isTrue();
        assertThat(properties.isPrivateOnlyAddress("169.254.2.3")).isTrue();
        assertThat(properties.isPrivateOnlyAddress("fd00::5")).isTrue();
        assertThat(properties.isPrivateOnlyAddress("203.0.113.7")).isFalse();
        assertThat(properties.isPrivateOnlyAddress("localhost")).isFalse();
    }

    @Test
    void parsesDumpRuntimeValues() {
        List<PeerRuntime> peers = WgDumpParser.parse("private\tpub\t51820\t0\n"
                + KEY + "\t(none)\t(none)\t10.8.0.2/32\t0\t12\t34\toff\n"
                + "x\t(none)\t1.2.3.4:5\t10.8.0.3/32\t1700000000\t5\t6\t25\n");
        assertThat(peers).hasSize(2);
        assertThat(peers.getFirst().endpoint()).isNull();
        assertThat(peers.getFirst().allowedIps()).containsExactly("10.8.0.2/32");
        assertThat(peers.getFirst().latestHandshake()).isNull();
        assertThat(peers.getFirst().persistentKeepalive()).isNull();
        assertThat(peers.get(1).latestHandshake()).isEqualTo(Instant.ofEpochSecond(1_700_000_000));
        assertThat(peers.get(1).persistentKeepalive()).isEqualTo(25);
    }

    @Test
    void encryptsPresharedKeys() {
        PskCipher cipher = new PskCipher(KEY);
        String encrypted = cipher.encrypt("psk-secret");
        assertThat(encrypted).isNotEqualTo("psk-secret");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("psk-secret");
        assertThatThrownBy(() -> new PskCipher("bad")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvesForwardedClientOnlyThroughTrustedProxyAndLimitsPerIp() {
        WireGuardProperties properties = new WireGuardProperties();
        properties.setTrustedProxies(List.of("192.0.2.1", "2001:db8:1::/48"));
        ClientIpResolver resolver = new ClientIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "198.51.100.8, 192.0.2.2");
        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.2");
        request.setRemoteAddr("203.0.113.10");
        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
        assertThat(resolver.isTrusted("2001:db8:1::9")).isTrue();
        assertThat(resolver.isTrusted("2001:db8:2::9")).isFalse();

        properties.setEnrollRateLimitPerMinute(1);
        EnrollRateLimiter limiter = new EnrollRateLimiter(properties,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        assertThat(limiter.acquire("198.51.100.1")).isZero();
        assertThat(limiter.acquire("198.51.100.1")).isPositive();
        assertThat(limiter.acquire("198.51.100.2")).isZero();
    }

    @Test
    void rejectsInvalidWgInputBeforeExecutingCommand(@TempDir Path temp) throws Exception {
        Path marker = temp.resolve("called");
        Path script = temp.resolve("wg");
        Files.writeString(script, "#!/bin/sh\nprintf called > '" + marker + "'\n");
        script.toFile().setExecutable(true);
        WireGuardProperties properties = properties(script);
        WgCliAdapter adapter = new WgCliAdapter(properties);
        assertThatThrownBy(() -> adapter.addPeer("invalid", "10.8.0.2/32", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.addPeer(KEY, "10.9.0.2/32", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Files.exists(marker)).isFalse();
    }

    @Test
    void removePeerUsesAnArgumentList(@TempDir Path temp) throws Exception {
        Path arguments = temp.resolve("args");
        Path script = temp.resolve("wg");
        Files.writeString(script, "#!/bin/sh\nprintf '%s\\n' \"$@\" > '" + arguments + "'\n");
        script.toFile().setExecutable(true);

        new WgCliAdapter(properties(script)).removePeer(KEY);

        assertThat(Files.readString(arguments).lines())
                .containsExactly("set", "wg0", "peer", KEY, "remove");
    }

    @Test
    void passesPresharedKeyOnlyOnStdinAndBuildsExactArguments(@TempDir Path temp) throws Exception {
        Path arguments = temp.resolve("args");
        Path stdin = temp.resolve("stdin");
        Path script = temp.resolve("wg");
        Files.writeString(script, "#!/bin/sh\nprintf '%s\\n' \"$@\" > '" + arguments
                + "'\ncat > '" + stdin + "'\n");
        script.toFile().setExecutable(true);
        WgCliAdapter adapter = new WgCliAdapter(properties(script));
        adapter.addPeer(KEY, "10.8.0.2/32", "secret-psk");
        assertThat(Files.readString(arguments).lines()).containsExactly("set", "wg0", "peer", KEY,
                "allowed-ips", "10.8.0.2/32", "preshared-key", "/dev/stdin");
        assertThat(Files.readString(stdin)).isEqualTo("secret-psk\n");
        assertThat(Files.readString(arguments)).doesNotContain("secret-psk");
    }

    @Test
    void includesStderrAndEnforcesCommandTimeout(@TempDir Path temp) throws Exception {
        Path script = temp.resolve("wg");
        Files.writeString(script, "#!/bin/sh\necho failing >&2\nexit 7\n");
        script.toFile().setExecutable(true);
        assertThatThrownBy(() -> new WgCliAdapter(properties(script)).listPeers())
                .hasMessageContaining("failing");
        Files.writeString(script, "#!/bin/sh\nsleep 2\n");
        WireGuardProperties properties = properties(script);
        properties.setCommandTimeout(Duration.ofMillis(50));
        assertThatThrownBy(() -> new WgCliAdapter(properties).listPeers())
                .hasMessageContaining("timed out");
    }

    private static WireGuardProperties properties(Path executable) {
        WireGuardProperties properties = new WireGuardProperties();
        properties.setWgCommand(executable.toString());
        properties.setNetwork("10.8.0.0/24");
        properties.setCommandTimeout(Duration.ofSeconds(2));
        return properties;
    }
}
