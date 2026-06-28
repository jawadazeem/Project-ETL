package com.azeem.blueprint.entity;

import jakarta.persistence.*;

/**
 * JPA entity representing a billing record stored in the database.
 *
 * <p>This class maps directly to the underlying billing_records table and contains the
 * persistence-level representation of a cloud billing entry. It is mutable and managed by
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
      @Index(name = "idx_billing_records_cloud_provider", columnList = "cloudProvider"),
      @Index(name = "idx_billing_records_service_name", columnList = "serviceName")
    })
public class BillingRecordEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dataset_id")
  private DatasetEntity dataset;

  @Column(nullable = false)
  private String billingPeriod;

  private String accountName;
  private String cloudProvider;
  private String resourceId;
  private double computeHours;
  private double storageGbUsed;
  private long apiRequests;
  private double totalCharge;
  private String serviceName;

  @Column(columnDefinition = "text")
  private String description;

  public BillingRecordEntity() {}

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public DatasetEntity getDataset() {
    return dataset;
  }

  public void setDataset(DatasetEntity dataset) {
    this.dataset = dataset;
  }

  public String getBillingPeriod() {
    return billingPeriod;
  }

  public void setBillingPeriod(String billingPeriod) {
    this.billingPeriod = billingPeriod;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getCloudProvider() {
    return cloudProvider;
  }

  public void setCloudProvider(String cloudProvider) {
    this.cloudProvider = cloudProvider;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public double getComputeHours() {
    return computeHours;
  }

  public void setComputeHours(double computeHours) {
    this.computeHours = computeHours;
  }

  public double getStorageGbUsed() {
    return storageGbUsed;
  }

  public void setStorageGbUsed(double storageGbUsed) {
    this.storageGbUsed = storageGbUsed;
  }

  public long getApiRequests() {
    return apiRequests;
  }

  public void setApiRequests(long apiRequests) {
    this.apiRequests = apiRequests;
  }

  public double getTotalCharge() {
    return totalCharge;
  }

  public void setTotalCharge(double totalCharge) {
    this.totalCharge = totalCharge;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
