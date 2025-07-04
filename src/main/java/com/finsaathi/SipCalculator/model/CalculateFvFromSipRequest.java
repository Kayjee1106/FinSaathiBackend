package com.finsaathi.SipCalculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects; // For equals/hashCode/toString

public class CalculateFvFromSipRequest {
    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("monthlySipAmount")
    private BigDecimal monthlySipAmount;

    @JsonProperty("timePeriodYears")
    private Integer timePeriodYears;

    @JsonProperty("dreamType")
    private String dreamType; // Optional, but included in DTO

    @JsonProperty("originalTargetFutureValue")
    private BigDecimal originalTargetFutureValue; // Optional, but included in DTO

    public CalculateFvFromSipRequest() {
        // Default constructor for Jackson
    }

    public CalculateFvFromSipRequest(UUID userId, BigDecimal monthlySipAmount, Integer timePeriodYears, String dreamType, BigDecimal originalTargetFutureValue) {
        this.userId = userId;
        this.monthlySipAmount = monthlySipAmount;
        this.timePeriodYears = timePeriodYears;
        this.dreamType = dreamType;
        this.originalTargetFutureValue = originalTargetFutureValue;
    }

    // --- Getters ---
    public UUID getUserId() { return userId; }
    public BigDecimal getMonthlySipAmount() { return monthlySipAmount; }
    public Integer getTimePeriodYears() { return timePeriodYears; }
    public String getDreamType() { return dreamType; }
    public BigDecimal getOriginalTargetFutureValue() { return originalTargetFutureValue; }

    // --- Setters ---
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setMonthlySipAmount(BigDecimal monthlySipAmount) { this.monthlySipAmount = monthlySipAmount; }
    public void setTimePeriodYears(Integer timePeriodYears) { this.timePeriodYears = timePeriodYears; }
    public void setDreamType(String dreamType) { this.dreamType = dreamType; }
    public void setOriginalTargetFutureValue(BigDecimal originalTargetFutureValue) { this.originalTargetFutureValue = originalTargetFutureValue; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalculateFvFromSipRequest that = (CalculateFvFromSipRequest) o;
        return Objects.equals(userId, that.userId) && Objects.equals(monthlySipAmount, that.monthlySipAmount) && Objects.equals(timePeriodYears, that.timePeriodYears) && Objects.equals(dreamType, that.dreamType) && Objects.equals(originalTargetFutureValue, that.originalTargetFutureValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, monthlySipAmount, timePeriodYears, dreamType, originalTargetFutureValue);
    }

    @Override
    public String toString() {
        return "CalculateFvFromSipRequest{" +
                "userId=" + userId +
                ", monthlySipAmount=" + monthlySipAmount +
                ", timePeriodYears=" + timePeriodYears +
                ", dreamType='" + dreamType + '\'' +
                ", originalTargetFutureValue=" + originalTargetFutureValue +
                '}';
    }
}