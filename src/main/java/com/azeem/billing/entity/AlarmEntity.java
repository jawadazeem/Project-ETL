package com.azeem.billing.entity;


import com.azeem.billing.model.AlarmScope;
import com.azeem.billing.model.AlarmSeverity;
import com.azeem.billing.model.Department;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an alarm stored in the database.
 *
 * <p>This class maps directly to the underlying alarms table and
 * contains the persistence-level representation of an alarm.
 * It is mutable and managed by JPA/Hibernate as part of the persistence context.
 *
 * <p>This entity should not contain business logic. All transformations between
 * this persistence model and the application's domain model are handled by the
 * AlarmMapper.
 */

@Entity
@Table(name = "alarms")
public class AlarmEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private AlarmScope alarmScope;
    private String billingPeriod;
    private String alarmType;
    private AlarmSeverity alarmSeverity;
    private String explanation;
    private Instant timestamp;
    private String employeeId;
    private String phoneNumber;
    private Department department;

    public AlarmEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AlarmScope getAlarmScope() {
        return alarmScope;
    }

    public void setAlarmScope(AlarmScope alarmScope) {
        this.alarmScope = alarmScope;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public AlarmSeverity getAlarmSeverity() {
        return alarmSeverity;
    }

    public void setAlarmSeverity(AlarmSeverity alarmSeverity) {
        this.alarmSeverity = alarmSeverity;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
