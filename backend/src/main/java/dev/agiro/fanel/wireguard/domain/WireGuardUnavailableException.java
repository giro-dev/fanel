package dev.agiro.fanel.wireguard.domain;

public class WireGuardUnavailableException extends RuntimeException {
    public WireGuardUnavailableException(String message) { super(message); }
    public WireGuardUnavailableException(String message, Throwable cause) { super(message, cause); }
}
