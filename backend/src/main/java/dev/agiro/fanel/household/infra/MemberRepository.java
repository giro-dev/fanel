package dev.agiro.fanel.household.infra;

import dev.agiro.fanel.household.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    List<Member> findAllByHouseholdId(UUID householdId);
}
