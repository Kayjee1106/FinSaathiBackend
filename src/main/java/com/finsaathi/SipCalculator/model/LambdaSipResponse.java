package com.finsaathi.SipCalculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public class LambdaSipResponse {
    @JsonProperty("time_in_years")
    private Integer timeInYears;

    @JsonProperty("future_value")
    private BigDecimal futureValue;
    @JsonProperty("monthly_sip_amount")
    private BigDecimal monthlySipAmount;

    @JsonProperty("calculated_best_weighted_annual_rate")
    private String calculatedBestWeightedAnnualRate;

    @JsonProperty("monthly_sip_required_best_weighted_case")
    private BigDecimal monthlySipRequiredBestWeightedCase;

    @JsonProperty("calculated_future_value")
    private BigDecimal calculatedFutureValue;

    @JsonProperty("rate_breakdown")
    private Map<String, BigDecimal> rateBreakdown;

    @JsonProperty("error")
    private String error;
    @JsonProperty("details")
    private String details;

    public LambdaSipResponse() {
        // Default constructor required by Jackson
    }

    public LambdaSipResponse(Integer timeInYears, BigDecimal futureValue, BigDecimal monthlySipAmount, String calculatedBestWeightedAnnualRate, BigDecimal monthlySipRequiredBestWeightedCase, BigDecimal calculatedFutureValue, Map<String, BigDecimal> rateBreakdown, String error, String details) {
        this.timeInYears = timeInYears;
        this.futureValue = futureValue;
        this.monthlySipAmount = monthlySipAmount;
        this.calculatedBestWeightedAnnualRate = calculatedBestWeightedAnnualRate;
        this.monthlySipRequiredBestWeightedCase = monthlySipRequiredBestWeightedCase;
        this.calculatedFutureValue = calculatedFutureValue;
        this.rateBreakdown = rateBreakdown;
        this.error = error;
        this.details = details;
    }

    // --- Getters ---
    public Integer getTimeInYears() {
        return timeInYears;
    }

    public BigDecimal getFutureValue() {
        return futureValue;
    }

    public BigDecimal getMonthlySipAmount() {
        return monthlySipAmount;
    }

    public String getCalculatedBestWeightedAnnualRate() {
        return calculatedBestWeightedAnnualRate;
    }

    public BigDecimal getMonthlySipRequiredBestWeightedCase() {
        return monthlySipRequiredBestWeightedCase;
    }

    public BigDecimal getCalculatedFutureValue() {
        return calculatedFutureValue;
    }

    public Map<String, BigDecimal> getRateBreakdown() {
        return rateBreakdown;
    }

    public String getError() {
        return error;
    }

    public String getDetails() {
        return details;
    }

    // --- Setters ---
    public void setTimeInYears(Integer timeInYears) {
        this.timeInYears = timeInYears;
    }

    public void setFutureValue(BigDecimal futureValue) {
        this.futureValue = futureValue;
    }

    public void setMonthlySipAmount(BigDecimal monthlySipAmount) {
        this.monthlySipAmount = monthlySipAmount;
    }

    public void setCalculatedBestWeightedAnnualRate(String calculatedBestWeightedAnnualRate) {
        this.calculatedBestWeightedAnnualRate = calculatedBestWeightedAnnualRate;
    }

    public void setMonthlySipRequiredBestWeightedCase(BigDecimal monthlySipRequiredBestWeightedCase) {
        this.monthlySipRequiredBestWeightedCase = monthlySipRequiredBestWeightedCase;
    }

    public void setCalculatedFutureValue(BigDecimal calculatedFutureValue) {
        this.calculatedFutureValue = calculatedFutureValue;
    }

    public void setRateBreakdown(Map<String, BigDecimal> rateBreakdown) {
        this.rateBreakdown = rateBreakdown;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LambdaSipResponse that = (LambdaSipResponse) o;
        return Objects.equals(timeInYears, that.timeInYears) &&
                Objects.equals(futureValue, that.futureValue) &&
                Objects.equals(monthlySipAmount, that.monthlySipAmount) &&
                Objects.equals(calculatedBestWeightedAnnualRate, that.calculatedBestWeightedAnnualRate) &&
                Objects.equals(monthlySipRequiredBestWeightedCase, that.monthlySipRequiredBestWeightedCase) &&
                Objects.equals(calculatedFutureValue, that.calculatedFutureValue) &&
                Objects.equals(rateBreakdown, that.rateBreakdown) &&
                Objects.equals(error, that.error) &&
                Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeInYears, futureValue, monthlySipAmount, calculatedBestWeightedAnnualRate, monthlySipRequiredBestWeightedCase, calculatedFutureValue, rateBreakdown, error, details);
    }

    @Override
    public String toString() {
        return "LambdaSipResponse{" +
                "timeInYears=" + timeInYears +
                ", futureValue=" + futureValue +
                ", monthlySipAmount=" + monthlySipAmount +
                ", calculatedBestWeightedAnnualRate='" + calculatedBestWeightedAnnualRate + '\'' +
                ", monthlySipRequiredBestWeightedCase=" + monthlySipRequiredBestWeightedCase +
                ", calculatedFutureValue=" + calculatedFutureValue +
                ", rateBreakdown=" + rateBreakdown +
                ", error='" + error + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
