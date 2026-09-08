package dev.agiro.fanel.household.api;

import java.util.UUID;

/** Published after a member is removed, so other modules can drop stale references to it. */
public record MemberDeleted(UUID householdId, UUID memberId) {}
