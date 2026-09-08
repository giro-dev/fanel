package dev.agiro.fanel.household.domain;

import dev.agiro.fanel.household.infra.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Authenticates household adults by their personal username/password, as an alternative to the
 * global admin account. Exposed as a plain {@link UserDetailsService} bean so that {@code shared}
 * can wire it into Spring Security without depending on this module's internals.
 */
@Service
public class MemberUserDetailsService implements UserDetailsService {
    private final MemberRepository members;

    public MemberUserDetailsService(MemberRepository members) {
        this.members = members;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Member member = members.findByUsername(username)
                .filter(Member::hasCredentials)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
        return new MemberUserDetails(member);
    }
}
