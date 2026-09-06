package dev.agiro.fanel.chores.infra;

import dev.agiro.fanel.chores.domain.Chore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChoreRepository extends JpaRepository<Chore, UUID> {
    List<Chore> findAllByHouseholdIdOrderByCreatedAtAsc(UUID householdId);
}
