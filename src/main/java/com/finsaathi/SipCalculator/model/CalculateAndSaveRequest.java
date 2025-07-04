package com.finsaathi.SipCalculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects; // For equals/hashCode/toString

public class CalculateAndSaveRequest {
    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("futureValue")
    private BigDecimal futureValue;

    @JsonProperty("timePeriodYears")
    private Integer timePeriodYears;

    @JsonProperty("dreamType")
    private String dreamType;

    public CalculateAndSaveRequest() {
        // Default constructor for Jackson
    }

    public CalculateAndSaveRequest(UUID userId, BigDecimal futureValue, Integer timePeriodYears, String dreamType) {
        this.userId = userId;
        this.futureValue = futureValue;
        this.timePeriodYears = timePeriodYears;
        this.dreamType = dreamType;
    }

    // --- Getters ---
    public UUID getUserId() { return userId; }
    public BigDecimal getFutureValue() { return futureValue; }
    public Integer getTimePeriodYears() { return timePeriodYears; }
    public String getDreamType() { return dreamType; }

    // --- Setters ---
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setFutureValue(BigDecimal futureValue) { this.futureValue = futureValue; }
    public void setTimePeriodYears(Integer timePeriodYears) { this.timePeriodYears = timePeriodYears; }
    public void setDreamType(String dreamType) { this.dreamType = dreamType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalculateAndSaveRequest that = (CalculateAndSaveRequest) o;
        return Objects.equals(userId, that.userId) && Objects.equals(futureValue, that.futureValue) && Objects.equals(timePeriodYears, that.timePeriodYears) && Objects.equals(dreamType, that.dreamType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, futureValue, timePeriodYears, dreamType);
    }

    @Override
    public String toString() {
        return "CalculateAndSaveRequest{" +
                "userId=" + userId +
                ", futureValue=" + futureValue +
                ", timePeriodYears=" + timePeriodYears +
                ", dreamType='" + dreamType + '\'' +
                '}';
    }
}