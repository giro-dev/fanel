package dev.agiro.fanel.wireguard.domain;

import dev.agiro.fanel.wireguard.infra.WireGuardProperties;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class EnrollRateLimiter {
    private final WireGuardProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public EnrollRateLimiter(WireGuardProperties properties, Clock wireGuardClock) {
        this.properties = properties;
        this.clock = wireGuardClock;
    }

    public Duration acquire(String address) {
        int capacity = properties.getEnrollRateLimitPerMinute();
        if (capacity <= 0) return Duration.ZERO;
        Instant now = clock.instant();
        Bucket bucket = buckets.computeIfAbsent(address, ignored -> new Bucket(capacity, now));
        synchronized (bucket) {
            double refillPerSecond = capacity / 60.0;
            double seconds = Math.max(0, Duration.between(bucket.updatedAt, now).toNanos() / 1_000_000_000.0);
            bucket.tokens = Math.min(capacity, bucket.tokens + seconds * refillPerSecond);
            bucket.updatedAt = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return Duration.ZERO;
            }
            long retryNanos = (long) Math.ceil((1 - bucket.tokens) / refillPerSecond * 1_000_000_000.0);
            return Duration.ofNanos(Math.max(1, retryNanos));
        }
    }

    private static final class Bucket {
        private double tokens;
        private Instant updatedAt;
        private Bucket(int capacity, Instant updatedAt) {
            this.tokens = capacity;
            this.updatedAt = updatedAt;
        }
    }
}
