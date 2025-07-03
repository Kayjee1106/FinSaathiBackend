package com.finsaathi.SipCalculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class SchemeSipSuggestion {

    @JsonProperty("product_type")
    private String productType;

    @JsonProperty("specific_name")
    private String specificName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("annual_rate") // The scheme's annual return rate (e.g., 0.08)
    private BigDecimal annualRate;

    @JsonProperty("monthly_sip_required_for_goal") // The calculated monthly SIP for THIS scheme to reach the user's goal
    private BigDecimal monthlySipRequiredForGoal;

    public SchemeSipSuggestion() {
    }

    public SchemeSipSuggestion(String productType, String specificName, String description, Integer year, BigDecimal annualRate, BigDecimal monthlySipRequiredForGoal) {
        this.productType = productType;
        this.specificName = specificName;
        this.description = description;
        this.year = year;
        this.annualRate = annualRate;
        this.monthlySipRequiredForGoal = monthlySipRequiredForGoal;
    }

    // --- Getters ---
    public String getProductType() { return productType; }
    public String getSpecificName() { return specificName; }
    public String getDescription() { return description; }
    public Integer getYear() { return year; }
    public BigDecimal getAnnualRate() { return annualRate; }
    public BigDecimal getMonthlySipRequiredForGoal() { return monthlySipRequiredForGoal; }

    // --- Setters ---
    public void setProductType(String productType) { this.productType = productType; }
    public void setSpecificName(String specificName) { this.specificName = specificName; }
    public void setDescription(String description) { this.description = description; }
    public void setYear(Integer year) { this.year = year; }
    public void setAnnualRate(BigDecimal annualRate) { this.annualRate = annualRate; }
    public void setMonthlySipRequiredForGoal(BigDecimal monthlySipRequiredForGoal) { this.monthlySipRequiredForGoal = monthlySipRequiredForGoal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemeSipSuggestion that = (SchemeSipSuggestion) o;
        return Objects.equals(specificName, that.specificName) && Objects.equals(year, that.year) && Objects.equals(monthlySipRequiredForGoal, that.monthlySipRequiredForGoal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(specificName, year, monthlySipRequiredForGoal);
    }

    @Override
    public String toString() {
        return "SchemeSipSuggestion{" +
                "productType='" + productType + '\'' +
                ", specificName='" + specificName + '\'' +
                ", description='" + description + '\'' +
                ", year=" + year +
                ", annualRate=" + annualRate +
                ", monthlySipRequiredForGoal=" + monthlySipRequiredForGoal +
                '}';
    }
}