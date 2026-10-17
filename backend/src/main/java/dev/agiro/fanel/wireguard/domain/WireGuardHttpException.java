package dev.agiro.fanel.wireguard.domain;

import org.springframework.http.HttpStatus;

public class WireGuardHttpException extends RuntimeException {
    private final HttpStatus status;
    public WireGuardHttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus status() { return status; }
}
