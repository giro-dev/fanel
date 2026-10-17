package dev.agiro.fanel.wireguard.domain;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class PskCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public PskCipher(String encodedKey) {
        byte[] bytes = Base64.getDecoder().decode(encodedKey);
        if (bytes.length != 32) throw new IllegalArgumentException("PSK encryption key must be 32 bytes");
        key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] output = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encrypt WireGuard PSK", exception);
        }
    }

    public String decrypt(String value) {
        try {
            byte[] input = Base64.getDecoder().decode(value);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, input, 0, 12));
            return new String(cipher.doFinal(input, 12, input.length - 12),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt WireGuard PSK", exception);
        }
    }
}
