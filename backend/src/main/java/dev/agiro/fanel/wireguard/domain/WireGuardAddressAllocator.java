package dev.agiro.fanel.wireguard.domain;

import java.util.Collection;
import java.util.Optional;

public final class WireGuardAddressAllocator {
    private WireGuardAddressAllocator() { }

    public static Optional<String> firstAvailable(Ipv4Cidr network, String serverAddress,
                                                   Collection<String> allocatedAddresses) {
        for (String address : network.hosts()) {
            if (!address.equals(serverAddress) && !allocatedAddresses.contains(address)) {
                return Optional.of(address);
            }
        }
        return Optional.empty();
    }
}
