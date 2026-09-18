package dev.agiro.fanel.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Convenience facade over the current {@link Authentication} for authorization checks: is the
 * caller the global admin or a household member with role ADMIN (full access), or a scoped
 * ADULT/CHILD member (see {@link MemberPrincipal}).
 */
@Component
public class CurrentAccess {
    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    public boolean isAdult() {
        return principal().map(MemberPrincipal::role).map(AccessRole.ADULT::equals).orElse(false);
    }

    public boolean isChild() {
        return principal().map(MemberPrincipal::role).map(AccessRole.CHILD::equals).orElse(false);
    }

    public Optional<UUID> memberId() {
        return principal().map(MemberPrincipal::memberId);
    }

    public Optional<UUID> householdId() {
        return principal().map(MemberPrincipal::householdId);
    }

    private Optional<MemberPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal member)) {
            return Optional.empty();
        }
        return Optional.of(member);
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }
}
