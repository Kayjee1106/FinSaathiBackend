package com.finsaathi.SipCalculator.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "goal_calculations")
public class GoalCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "calculated_best_weighted_annual_rate", precision = 5, scale = 4)
    private BigDecimal calculatedBestWeightedAnnualRate;

    @Column(name = "monthly_sip_required_best_weighted_case", precision = 19, scale = 2)
    private BigDecimal monthlySipRequiredBestWeightedCase;

    @Column(name = "calculated_future_value_from_user_sip", precision = 19, scale = 2)
    private BigDecimal calculatedFutureValueFromUserSip;

    @Column(name = "aws_lambda_response_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String awsLambdaResponseJson;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    public GoalCalculation() {
        // Default constructor required by JPA
    }

    public GoalCalculation(UUID id, UUID requestId, BigDecimal calculatedBestWeightedAnnualRate, BigDecimal monthlySipRequiredBestWeightedCase, BigDecimal calculatedFutureValueFromUserSip, String awsLambdaResponseJson, LocalDateTime calculatedAt) {
        this.id = id;
        this.requestId = requestId;
        this.calculatedBestWeightedAnnualRate = calculatedBestWeightedAnnualRate;
        this.monthlySipRequiredBestWeightedCase = monthlySipRequiredBestWeightedCase;
        this.calculatedFutureValueFromUserSip = calculatedFutureValueFromUserSip;
        this.awsLambdaResponseJson = awsLambdaResponseJson;
        this.calculatedAt = calculatedAt;
    }

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    // --- Getters ---
    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public BigDecimal getCalculatedBestWeightedAnnualRate() {
        return calculatedBestWeightedAnnualRate;
    }

    public BigDecimal getMonthlySipRequiredBestWeightedCase() {
        return monthlySipRequiredBestWeightedCase;
    }

    public BigDecimal getCalculatedFutureValueFromUserSip() {
        return calculatedFutureValueFromUserSip;
    }

    public String getAwsLambdaResponseJson() {
        return awsLambdaResponseJson;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    // --- Setters ---
    public void setId(UUID id) {
        this.id = id;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public void setCalculatedBestWeightedAnnualRate(BigDecimal calculatedBestWeightedAnnualRate) {
        this.calculatedBestWeightedAnnualRate = calculatedBestWeightedAnnualRate;
    }

    public void setMonthlySipRequiredBestWeightedCase(BigDecimal monthlySipRequiredBestWeightedCase) {
        this.monthlySipRequiredBestWeightedCase = monthlySipRequiredBestWeightedCase;
    }

    public void setCalculatedFutureValueFromUserSip(BigDecimal calculatedFutureValueFromUserSip) {
        this.calculatedFutureValueFromUserSip = calculatedFutureValueFromUserSip;
    }

    public void setAwsLambdaResponseJson(String awsLambdaResponseJson) {
        this.awsLambdaResponseJson = awsLambdaResponseJson;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoalCalculation that = (GoalCalculation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GoalCalculation{" +
                "id=" + id +
                ", requestId=" + requestId +
                ", calculatedBestWeightedAnnualRate=" + calculatedBestWeightedAnnualRate +
                ", monthlySipRequiredBestWeightedCase=" + monthlySipRequiredBestWeightedCase +
                ", calculatedFutureValueFromUserSip=" + calculatedFutureValueFromUserSip +
                ", awsLambdaResponseJson='" + awsLambdaResponseJson + '\'' +
                ", calculatedAt=" + calculatedAt +
                '}';
    }
}
