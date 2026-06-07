/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "datasets")
public class DatasetEntity {
  @Id private UUID id;

  @ManyToOne
  @JoinColumn(name = "owner_user_id")
  private AppUserEntity ownerUser;

  @Column(name = "billing_period")
  private String billingPeriod;

  @Column(name = "source_filename")
  private String sourceFilename;

  @Column(name = "s3_object_key")
  private String s3ObjectKey;

  @Column(name = "uploaded_at")
  private Instant uploadedAt;

  private String status;

  @Column(name = "archived", nullable = false)
  private boolean archived = false;

  @PrePersist
  void ensureId() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }

  public UUID getId() {
    return id;
  }

  public AppUserEntity getOwnerUser() {
    return ownerUser;
  }

  public String getBillingPeriod() {
    return billingPeriod;
  }

  public String getSourceFilename() {
    return sourceFilename;
  }

  public String getS3ObjectKey() {
    return s3ObjectKey;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setOwnerUser(AppUserEntity ownerUser) {
    this.ownerUser = ownerUser;
  }

  public void setBillingPeriod(String billingPeriod) {
    this.billingPeriod = billingPeriod;
  }

  public void setSourceFilename(String sourceFilename) {
    this.sourceFilename = sourceFilename;
  }

  public void setS3ObjectKey(String s3ObjectKey) {
    this.s3ObjectKey = s3ObjectKey;
  }

  public void setUploadedAt(Instant uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }
}
