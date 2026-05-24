package com.azeem.blueprint.entity;

import jakarta.persistence.*;

/**
 * JPA entity representing a billing record stored in the database.
 *
 * <p>This class maps directly to the underlying billing_records table and contains the
 * persistence-level representation of a telecom billing entry. It is mutable and managed by
 * JPA/Hibernate as part of the persistence context.
 *
 * <p>This entity should not contain business logic. All transformations between this persistence
 * model and the application's domain model are handled by the BillingRecordMapper.
 */
@Entity
@Table(
    name = "billing_records",
    indexes = {
      @Index(name = "idx_billing_period", columnList = "billingPeriod"),
      @Index(name = "idx_total_charge_desc", columnList = "totalCharge DESC"),
      @Index(name = "idx_billing_records_dataset_id", columnList = "dataset_id"),
      @Index(name = "idx_billing_records_dataset_period", columnList = "dataset_id, billingPeriod"),
      @Index(name = "idx_billing_records_department", columnList = "department")
    })
public class BillingRecordEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id; // Auto-generated primary key by DB

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dataset_id")
  private DatasetEntity dataset;

  @Column(nullable = false)
  private String billingPeriod;

  private String accountName;
  private String department;
  private String employeeId;
  private String phoneNumber;
  private int minutesUsed;
  private double dataGbUsed;
  private int smsCount;
  private double totalCharge;

  public BillingRecordEntity() {}

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
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

  public DatasetEntity getDataset() {
    return dataset;
  }

  public void setDataset(DatasetEntity dataset) {
    this.dataset = dataset;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }
}
