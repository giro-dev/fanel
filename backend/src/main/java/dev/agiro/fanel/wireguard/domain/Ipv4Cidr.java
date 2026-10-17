package dev.agiro.fanel.wireguard.domain;

public record Ipv4Cidr(long networkAddress, int prefix) {
    public static Ipv4Cidr parse(String value) {
        if (value == null) throw new IllegalArgumentException("IPv4 CIDR is required");
        String[] parts = value.split("/", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid IPv4 CIDR");
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid IPv4 prefix", exception);
        }
        long address = ipv4(parts[0]);
        if (prefix < 0 || prefix > 32) throw new IllegalArgumentException("Invalid IPv4 prefix");
        long mask = prefix == 0 ? 0 : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
        return new Ipv4Cidr(address & mask, prefix);
    }

    public static long ipv4(String address) {
        if (address == null || !address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            throw new IllegalArgumentException("Invalid IPv4 address");
        }
        long result = 0;
        for (String octet : address.split("\\.")) {
            int value = Integer.parseInt(octet);
            if (value > 255) throw new IllegalArgumentException("Invalid IPv4 address");
            result = (result << 8) | value;
        }
        return result;
    }

    public static String address(long value) {
        return ((value >>> 24) & 255) + "." + ((value >>> 16) & 255) + "."
                + ((value >>> 8) & 255) + "." + (value & 255);
    }

    public boolean contains(String address) {
        long parsed;
        try {
            parsed = ipv4(address);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        long mask = prefix == 0 ? 0 : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
        return (parsed & mask) == networkAddress;
    }

    public Iterable<String> hosts() {
        long end = networkAddress + (1L << (32 - prefix)) - 1;
        return () -> new java.util.Iterator<>() {
            private long next = networkAddress + 1;
            @Override public boolean hasNext() { return next < end; }
            @Override public String next() {
                if (!hasNext()) throw new java.util.NoSuchElementException();
                return address(next++);
            }
        };
    }

    public int prefix() { return prefix; }
    public String network() { return address(networkAddress) + "/" + prefix; }
}
