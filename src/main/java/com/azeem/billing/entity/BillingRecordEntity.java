package com.azeem.billing.entity;

import jakarta.persistence.*;

/**
 * JPA entity representing a billing record stored in the database.
 *
 * <p>This class maps directly to the underlying billing_records table and
 * contains the persistence-level representation of a telecom billing entry.
 * It is mutable and managed by JPA/Hibernate as part of the persistence context.
 *
 * <p>This entity should not contain business logic. All transformations between
 * this persistence model and the application's domain model are handled by the
 * BillingRecordMapper.
 */

@Entity
@Table(name="billing_records")
public class BillingRecordEntity {
    @Id
    @GeneratedValue
    private long id; // Auto-generated primary key by DB

    private String accountName;
    private String employeeId;
    private String department;
    private String phoneNumber;
    private String billingPeriod;
    private int minutesUsed;
    private double dataGbUsed;
    private int smsCount;
    private double totalCharge;


    public BillingRecordEntity() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public int getMinutesUsed() {
        return minutesUsed;
    }

    public void setMinutesUsed(int minutesUsed) {
        this.minutesUsed = minutesUsed;
    }

    public double getDataGbUsed() {
        return dataGbUsed;
    }

    public void setDataGbUsed(double dataGbUsed) {
        this.dataGbUsed = dataGbUsed;
    }

    public int getSmsCount() {
        return smsCount;
    }

    public void setSmsCount(int smsCount) {
        this.smsCount = smsCount;
    }

    public double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(double totalCharge) {
        this.totalCharge = totalCharge;
    }
}
