package dev.agiro.fanel.shared.security;

import java.util.UUID;

/**
 * Implemented by the {@code UserDetails} of an authenticated household member (as opposed to the
 * global bootstrap admin, which has no associated member/household).
 */
public interface MemberPrincipal {
    UUID memberId();
    UUID householdId();
    AccessRole role();
}
