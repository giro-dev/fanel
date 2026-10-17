package dev.agiro.fanel.wireguard.web;

import dev.agiro.fanel.wireguard.api.EnrollRequest;
import dev.agiro.fanel.wireguard.api.EnrollResponse;
import dev.agiro.fanel.wireguard.api.ServerIdentity;
import dev.agiro.fanel.wireguard.domain.ClientIpResolver;
import dev.agiro.fanel.wireguard.domain.EnrollRateLimiter;
import dev.agiro.fanel.wireguard.domain.WireGuardHttpException;
import dev.agiro.fanel.wireguard.domain.WireGuardPeerService;
import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WireGuardController {
    private static final Logger audit = LoggerFactory.getLogger("fanel.audit.wireguard");
    private final WireGuardPeerService service;
    private final WireGuardProperties properties;
    private final ClientIpResolver ipResolver;
    private final EnrollRateLimiter limiter;

    public WireGuardController(WireGuardPeerService service, WireGuardProperties properties,
                               ClientIpResolver ipResolver, EnrollRateLimiter limiter) {
        this.service = service;
        this.properties = properties;
        this.ipResolver = ipResolver;
        this.limiter = limiter;
    }

    @PostMapping("/wg/enroll")
    public EnrollResponse enroll(@RequestBody EnrollRequest request, HttpServletRequest servletRequest) {
        if (!properties.isEnabled()) {
            throw new WireGuardHttpException(HttpStatus.NOT_FOUND, "WireGuard is disabled");
        }
        String clientIp = ipResolver.resolve(servletRequest);
        if (properties.isEnrollPrivateOnly() && !properties.isPrivateOnlyAddress(clientIp)) {
            audit.warn("enroll rejected category=non-private client={}", clientIp);
            throw new WireGuardHttpException(HttpStatus.FORBIDDEN, "Enrollment is limited to private network addresses");
        }
        Duration retry = limiter.acquire(clientIp);
        if (!retry.isZero()) {
            audit.warn("enroll rejected category=rate-limit client={}", clientIp);
            throw new RateLimitedException(retry);
        }
        return service.enroll(request, clientIp);
    }

    @DeleteMapping("/wg/peers/me")
    public ResponseEntity<Void> revokeMe(HttpServletRequest request,
                                         @RequestHeader(value = "X-Fanel-Wg-Public-Key", required = false) String key) {
        service.revokeMe(request.getRemoteAddr(), key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health/identity")
    public ServerIdentity identity() {
        return service.identity();
    }

    public static class RateLimitedException extends RuntimeException {
        private final long retryAfter;
        public RateLimitedException(Duration retry) {
            super("Enrollment rate limit exceeded");
            retryAfter = Math.max(1, (retry.toSeconds() + 1));
        }
        public long retryAfter() { return retryAfter; }
    }
}
