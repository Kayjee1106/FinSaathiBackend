package com.finsaathi.SipCalculator.repository;

import com.finsaathi.SipCalculator.model.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, UUID> {
    List<UserRequest> findByUserId(UUID userId);
    List<UserRequest> findByFutureValueAndTimePeriodYearsOrderByRequestTimestampAsc(BigDecimal futureValue, Integer timePeriodYears);
}
