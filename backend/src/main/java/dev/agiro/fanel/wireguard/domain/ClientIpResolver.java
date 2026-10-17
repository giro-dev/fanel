package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {
    private final WireGuardProperties properties;
    public ClientIpResolver(WireGuardProperties properties) { this.properties = properties; }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (remote == null || !isTrusted(remote)) return remote;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return remote;
        String[] addresses = forwarded.split(",");
        for (int i = addresses.length - 1; i >= 0; i--) {
            String candidate = addresses[i].trim();
            if (!candidate.isBlank() && !isTrusted(candidate)) return candidate;
        }
        return remote;
    }

    public boolean isTrusted(String address) {
        for (String proxy : properties.getTrustedProxies()) {
            String candidate = proxy.trim();
            if (candidate.contains("/") && IpAddress.isInCidr(address, candidate)) return true;
            if (!candidate.contains("/")) {
                try {
                    if (java.util.Arrays.equals(IpAddress.parseNumeric(candidate), IpAddress.parseNumeric(address))) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return false;
    }
}
