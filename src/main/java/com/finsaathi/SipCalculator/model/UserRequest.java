package com.finsaathi.SipCalculator.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_requests")
public class UserRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // This is the Transaction ID

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "dream_type")
    private String dreamType;

    @Column(name = "future_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal futureValue; // This stores the Goal Value for calculate_sip_from_fv, OR the Monthly SIP for calculate_fv_from_sip

    @Column(name = "time_period_years", nullable = false)
    private Integer timePeriodYears;

    @Column(name = "original_target_future_value", precision = 19, scale = 2) // NEW COLUMN
    private BigDecimal originalTargetFutureValue;

    @Column(name = "request_timestamp", nullable = false, updatable = false)
    private LocalDateTime requestTimestamp;

    public UserRequest() {
        // Default constructor required by JPA
    }

    public UserRequest(UUID id, UUID userId, String dreamType, BigDecimal futureValue, Integer timePeriodYears, BigDecimal originalTargetFutureValue, LocalDateTime requestTimestamp) {
        this.id = id;
        this.userId = userId;
        this.dreamType = dreamType;
        this.futureValue = futureValue;
        this.timePeriodYears = timePeriodYears;
        this.originalTargetFutureValue = originalTargetFutureValue; // NEW FIELD IN CONSTRUCTOR
        this.requestTimestamp = requestTimestamp;
    }

    @PrePersist
    protected void onCreate() {
        requestTimestamp = LocalDateTime.now();
    }

    // --- Getters ---
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDreamType() { return dreamType; }
    public BigDecimal getFutureValue() { return futureValue; }
    public Integer getTimePeriodYears() { return timePeriodYears; }
    public BigDecimal getOriginalTargetFutureValue() { return originalTargetFutureValue; } // NEW GETTER
    public LocalDateTime getRequestTimestamp() { return requestTimestamp; }

    // --- Setters ---
    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setDreamType(String dreamType) { this.dreamType = dreamType; }
    public void setFutureValue(BigDecimal futureValue) { this.futureValue = futureValue; }
    public void setTimePeriodYears(Integer timePeriodYears) { this.timePeriodYears = timePeriodYears; }
    public void setOriginalTargetFutureValue(BigDecimal originalTargetFutureValue) { this.originalTargetFutureValue = originalTargetFutureValue; } // NEW SETTER
    public void setRequestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRequest that = (UserRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserRequest{" +
                "id=" + id +
                ", userId=" + userId +
                ", dreamType='" + dreamType + '\'' +
                ", futureValue=" + futureValue +
                ", timePeriodYears=" + timePeriodYears +
                ", originalTargetFutureValue=" + originalTargetFutureValue + // Include in toString
                ", requestTimestamp=" + requestTimestamp +
                '}';
    }
}