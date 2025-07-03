package com.finsaathi.SipCalculator.repository;

import com.finsaathi.SipCalculator.model.GoalCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalCalculationRepository extends JpaRepository<GoalCalculation, UUID> {
    Optional<GoalCalculation> findByRequestId(UUID requestId);
}
