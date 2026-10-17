package dev.agiro.fanel.wireguard.domain;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ActivePeerAddresses {
    private final AtomicReference<Set<String>> addresses = new AtomicReference<>(Set.of());
    public boolean contains(String address) { return addresses.get().contains(address); }
    public void replace(Set<String> value) { addresses.set(Set.copyOf(value)); }
}
