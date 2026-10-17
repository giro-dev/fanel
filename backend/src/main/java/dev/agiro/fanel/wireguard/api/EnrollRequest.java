package dev.agiro.fanel.wireguard.api;

public record EnrollRequest(String token, String publicKey, String deviceName) { }
