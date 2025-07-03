package com.finsaathi.SipCalculator.repository;

import com.finsaathi.SipCalculator.model.InvestmentSchemeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentSchemeRateRepository extends JpaRepository<InvestmentSchemeRate, UUID> {
    List<InvestmentSchemeRate> findByYear(Integer year);
    Optional<InvestmentSchemeRate> findBySpecificNameAndYear(String specificName, Integer year);
}
