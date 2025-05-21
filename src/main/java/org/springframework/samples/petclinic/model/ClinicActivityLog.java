package org.springframework.samples.petclinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for logging clinic activities for analysis.
 * Represents an entry with various data types, where 'activityDescription' (formerly testString)
 * is intentionally not indexed for performance observation on queries.
 */
@Entity
@Table(name = "clinic_activity_logs")
public class ClinicActivityLog extends BaseEntity {

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "numeric_value")
    private Integer numericValue;

    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @Column(name = "status_flag")
    private Boolean statusFlag;

    @Column(name = "payload")
    private String payload;

    // Getters and Setters

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Integer getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Integer numericValue) {
        this.numericValue = numericValue;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Boolean getStatusFlag() {
        return statusFlag;
    }

    public void setStatusFlag(Boolean statusFlag) {
        this.statusFlag = statusFlag;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "ClinicActivityLog{" +
                "id=" + getId() +
                ", activityType='" + activityType + '\'' +
                ", numericValue=" + numericValue +
                ", eventTimestamp=" + eventTimestamp +
                ", statusFlag=" + statusFlag +
                ", payloadLength=" + (payload != null ? payload.length() : 0) +
                '}';
    }
} 