package dev.agiro.fanel.assistant.domain;

public record ModelProfile(String provider, String model, Double temperature, Integer maxTokens) {
}
