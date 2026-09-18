package dev.agiro.fanel.household.domain;

import dev.agiro.fanel.shared.security.AccessRole;
import dev.agiro.fanel.shared.security.MemberPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** {@link UserDetails} for a household member logging in with their own username/password. */
class MemberUserDetails implements UserDetails, MemberPrincipal {
    private final UUID memberId;
    private final UUID householdId;
    private final String username;
    private final String passwordHash;
    private final AccessRole role;

    MemberUserDetails(Member member) {
        this.memberId = member.getId();
        this.householdId = member.getHousehold().getId();
        this.username = member.getUsername();
        this.passwordHash = member.getPasswordHash();
        this.role = AccessRole.valueOf(member.getRole().name());
    }

    @Override
    public UUID memberId() { return memberId; }
    @Override
    public UUID householdId() { return householdId; }
    @Override
    public AccessRole role() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }
    @Override
    public String getUsername() { return username; }
}
