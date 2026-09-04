package dev.agiro.fanel.household.infra;

import dev.agiro.fanel.household.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {}
