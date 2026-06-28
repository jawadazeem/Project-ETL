package com.azeem.blueprint.entity;

import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.CloudProvider;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an alarm stored in the database.
 *
 * <p>This class maps directly to the underlying alarms table and contains the persistence-level
 * representation of an alarm. It is mutable and managed by JPA/Hibernate as part of the persistence
 * context.
 *
 * <p>This entity should not contain business logic. All transformations between this persistence
 * model and the application's domain model are handled by the AlarmMapper.
 */
@Entity
@Table(
    name = "alarms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id", "business_key"}))
public class AlarmEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dataset_id")
  private DatasetEntity dataset;

  @Column(name = "business_key", nullable = false)
  private UUID businessKey;

  private @Enumerated(EnumType.STRING) AlarmScope alarmScope;
  private String billingPeriod;
  private String alarmType;
  private @Enumerated(EnumType.STRING) AlarmSeverity alarmSeverity;
  private String explanation;
  private Instant timestamp;
  private String resourceId;
  private String serviceName;
  private @Enumerated(EnumType.STRING) CloudProvider cloudProvider;

  public AlarmEntity() {}

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

  public CloudProvider getCloudProvider() {
    return cloudProvider;
  }

  public void setCloudProvider(CloudProvider cloudProvider) {
    this.cloudProvider = cloudProvider;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
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

  public UUID getBusinessKey() {
    return businessKey;
  }

  public void setBusinessKey(UUID businessKey) {
    this.businessKey = businessKey;
  }

  public DatasetEntity getDataset() {
    return dataset;
  }

  public void setDataset(DatasetEntity dataset) {
    this.dataset = dataset;
  }
}
