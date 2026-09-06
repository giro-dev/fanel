package dev.agiro.fanel.household.domain;

import dev.agiro.fanel.household.api.MemberRole;
import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "member")
public class Member extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    private String name;
    @Enumerated(EnumType.STRING)
    private MemberRole role;
    private String color;
    private String pin;
    private Instant createdAt;

    protected Member() {}

    public Member(Household household, String name, MemberRole role, String color) {
        this.household = household;
        this.name = name;
        this.role = role;
        this.color = color;
        this.createdAt = Instant.now();
    }

    public Household getHousehold() { return household; }
    public String getName() { return name; }
    public MemberRole getRole() { return role; }
    public String getColor() { return color; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean hasPin() { return pin != null && !pin.isBlank(); }
    public boolean matchesPin(String candidate) { return !hasPin() || pin.equals(candidate); }
    public void setPin(String pin) { this.pin = pin; }
}
