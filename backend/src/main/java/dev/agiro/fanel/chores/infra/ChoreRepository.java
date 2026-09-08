package dev.agiro.fanel.chores.infra;

import dev.agiro.fanel.chores.domain.Chore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChoreRepository extends JpaRepository<Chore, UUID> {
    List<Chore> findAllByHouseholdIdOrderByCreatedAtAsc(UUID householdId);

    @Modifying
    @Query("update Chore c set c.assigneeId = null where c.assigneeId = :memberId")
    void clearAssignee(@Param("memberId") UUID memberId);
}
