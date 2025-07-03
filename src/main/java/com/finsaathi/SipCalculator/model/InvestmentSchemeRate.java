package com.finsaathi.SipCalculator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "investment_scheme_rates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"specific_name", "year"})
})
public class InvestmentSchemeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "specific_name", nullable = false)
    private String specificName;

    @Column(name = "description")
    private String description;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal annualRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InvestmentSchemeRate() {
        // Default constructor required by JPA
    }

    public InvestmentSchemeRate(UUID id, String productType, String specificName, String description, Integer year, BigDecimal annualRate, LocalDateTime createdAt) {
        this.id = id;
        this.productType = productType;
        this.specificName = specificName;
        this.description = description;
        this.year = year;
        this.annualRate = annualRate;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters ---
    public UUID getId() {
        return id;
    }

    public String getProductType() {
        return productType;
    }

    public String getSpecificName() {
        return specificName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getYear() {
        return year;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // --- Setters ---
    public void setId(UUID id) {
        this.id = id;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public void setSpecificName(String specificName) {
        this.specificName = specificName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setAnnualRate(BigDecimal annualRate) {
        this.annualRate = annualRate;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvestmentSchemeRate that = (InvestmentSchemeRate) o;
        return Objects.equals(specificName, that.specificName) && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(specificName, year);
    }

    @Override
    public String toString() {
        return "InvestmentSchemeRate{" +
                "id=" + id +
                ", productType='" + productType + '\'' +
                ", specificName='" + specificName + '\'' +
                ", description='" + description + '\'' +
                ", year=" + year +
                ", annualRate=" + annualRate +
                ", createdAt=" + createdAt +
                '}';
    }
}
