package dev.agiro.fanel.wireguard.domain;

import java.util.Base64;

public final class WgKeys {
    private WgKeys() { }

    public static void validatePublicKey(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9+/]{43}=$")) {
            throw new IllegalArgumentException("Invalid WireGuard public key");
        }
        try {
            if (Base64.getDecoder().decode(key).length != 32) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid WireGuard public key", exception);
        }
    }
}
