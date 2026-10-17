package dev.agiro.fanel.wireguard.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpAddress {
    private IpAddress() { }

    public static byte[] parseNumeric(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("IP address is required");
        if (value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            long parsed = Ipv4Cidr.ipv4(value);
            return new byte[] { (byte) (parsed >>> 24), (byte) (parsed >>> 16),
                    (byte) (parsed >>> 8), (byte) parsed };
        }
        if (!value.contains(":") || !value.matches("[0-9a-fA-F:.]+")) {
            throw new IllegalArgumentException("Invalid IP address");
        }
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid IP address", exception);
        }
    }

    public static boolean isInCidr(String address, String cidr) {
        try {
            String[] parts = cidr.split("/", -1);
            if (parts.length != 2) return false;
            byte[] candidate = parseNumeric(address);
            byte[] network = parseNumeric(parts[0]);
            if (candidate.length != network.length) return false;
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > candidate.length * 8) return false;
            int wholeBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < wholeBytes; i++) {
                if (candidate[i] != network[i]) return false;
            }
            if (remainingBits == 0) return true;
            int mask = 0xff << (8 - remainingBits);
            return (candidate[wholeBytes] & mask) == (network[wholeBytes] & mask);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
