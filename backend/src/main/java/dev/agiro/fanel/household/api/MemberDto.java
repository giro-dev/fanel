package dev.agiro.fanel.household.api;

import dev.agiro.fanel.household.domain.Member;
import dev.agiro.fanel.household.domain.MemberRole;

import java.time.Instant;
import java.util.UUID;

public record MemberDto(UUID id, UUID householdId, String name, MemberRole role, String color, Instant createdAt) {
    public static MemberDto from(Member member) {
        return new MemberDto(member.getId(), member.getHousehold().getId(), member.getName(),
                member.getRole(), member.getColor(), member.getCreatedAt());
    }
}
