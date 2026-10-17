package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.api.PeerRuntime;
import dev.agiro.fanel.wireguard.api.WireGuardAdapter;
import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WgCliAdapter implements WireGuardAdapter {
    private static final Logger log = LoggerFactory.getLogger(WgCliAdapter.class);
    private final WireGuardProperties properties;
    private volatile String cachedServerPublicKey;

    public WgCliAdapter(WireGuardProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fetchServerKeyOnStartup() {
        try {
            String key = run(List.of(properties.getWgCommand(), "show",
                    properties.getInterfaceName(), "public-key"), null).trim();
            WgKeys.validatePublicKey(key);
            cachedServerPublicKey = key;
        } catch (RuntimeException exception) {
            cachedServerPublicKey = null;
            log.error("Unable to read WireGuard server public key at startup; WireGuard remains available for retry", exception);
        }
    }

    @Override
    public String serverPublicKey() {
        String key = cachedServerPublicKey;
        if (key != null) return key;
        try {
            key = run(List.of(properties.getWgCommand(), "show",
                    properties.getInterfaceName(), "public-key"), null).trim();
            WgKeys.validatePublicKey(key);
            cachedServerPublicKey = key;
            return key;
        } catch (RuntimeException exception) {
            log.error("Unable to read WireGuard server public key", exception);
            return null;
        }
    }

    @Override
    public void addPeer(String publicKey, String allowedIp, String presharedKey) {
        WgKeys.validatePublicKey(publicKey);
        if (allowedIp == null || !allowedIp.endsWith("/32")
                || !Ipv4Cidr.parse(properties.getNetwork()).contains(allowedIp.substring(0, allowedIp.length() - 3))) {
            throw new IllegalArgumentException("Allowed IP must be a host inside the configured network");
        }
        List<String> args = new ArrayList<>(List.of(properties.getWgCommand(), "set",
                properties.getInterfaceName(), "peer", publicKey, "allowed-ips", allowedIp));
        if (presharedKey != null) args.addAll(List.of("preshared-key", "/dev/stdin"));
        run(args, presharedKey == null ? null : presharedKey + "\n");
    }

    @Override
    public void removePeer(String publicKey) {
        WgKeys.validatePublicKey(publicKey);
        run(List.of(properties.getWgCommand(), "set", properties.getInterfaceName(),
                "peer", publicKey, "remove"), null);
    }

    @Override
    public List<PeerRuntime> listPeers() {
        String dump = run(List.of(properties.getWgCommand(), "show",
                properties.getInterfaceName(), "dump"), null);
        return WgDumpParser.parse(dump);
    }

    private String run(List<String> command, String stdin) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException exception) {
            throw new WireGuardUnavailableException("Could not execute WireGuard command", exception);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> {
            try (var stream = process.getInputStream()) {
                stream.transferTo(output);
            } catch (IOException ignored) { }
        });
        try {
            try (var stream = process.getOutputStream()) {
                if (stdin != null) stream.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
            Duration timeout = properties.getCommandTimeout();
            if (!process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                reader.join();
                throw new WireGuardUnavailableException("WireGuard command timed out after " + timeout
                        + ": " + new String(output.toByteArray(), StandardCharsets.UTF_8).trim());
            }
            reader.join();
            String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new WireGuardUnavailableException("WireGuard command exited with " + process.exitValue()
                        + ": " + text.trim());
            }
            return text;
        } catch (IOException exception) {
            process.destroyForcibly();
            throw new WireGuardUnavailableException("Failed to communicate with WireGuard command", exception);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new WireGuardUnavailableException("Interrupted while running WireGuard command", exception);
        }
    }
}
