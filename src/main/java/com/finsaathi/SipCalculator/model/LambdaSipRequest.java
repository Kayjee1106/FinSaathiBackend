package com.finsaathi.SipCalculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

public class LambdaSipRequest {
    @JsonProperty("operation")
    private String operation;

    @JsonProperty("time_in_years")
    private Integer timeInYears;

    @JsonProperty("future_value")
    private BigDecimal futureValue;
    @JsonProperty("monthly_sip_amount")
    private BigDecimal monthlySipAmount;

    public LambdaSipRequest() {
        // Default constructor required by Jackson
    }

    public LambdaSipRequest(String operation, Integer timeInYears, BigDecimal futureValue, BigDecimal monthlySipAmount) {
        this.operation = operation;
        this.timeInYears = timeInYears;
        this.futureValue = futureValue;
        this.monthlySipAmount = monthlySipAmount;
    }

    // --- Getters ---
    public String getOperation() {
        return operation;
    }

    public Integer getTimeInYears() {
        return timeInYears;
    }

    public BigDecimal getFutureValue() {
        return futureValue;
    }

    public BigDecimal getMonthlySipAmount() {
        return monthlySipAmount;
    }

    // --- Setters ---
    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setTimeInYears(Integer timeInYears) {
        this.timeInYears = timeInYears;
    }

    public void setFutureValue(BigDecimal futureValue) {
        this.futureValue = futureValue;
    }

    public void setMonthlySipAmount(BigDecimal monthlySipAmount) {
        this.monthlySipAmount = monthlySipAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LambdaSipRequest that = (LambdaSipRequest) o;
        return Objects.equals(operation, that.operation) &&
                Objects.equals(timeInYears, that.timeInYears) &&
                Objects.equals(futureValue, that.futureValue) &&
                Objects.equals(monthlySipAmount, that.monthlySipAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, timeInYears, futureValue, monthlySipAmount);
    }

    @Override
    public String toString() {
        return "LambdaSipRequest{" +
                "operation='" + operation + '\'' +
                ", timeInYears=" + timeInYears +
                ", futureValue=" + futureValue +
                ", monthlySipAmount=" + monthlySipAmount +
                '}';
    }
}
