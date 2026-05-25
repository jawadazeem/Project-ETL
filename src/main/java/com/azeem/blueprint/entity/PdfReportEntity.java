/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pdf_reports")
public class PdfReportEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dataset_id")
  private DatasetEntity dataset;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUserEntity user;

  @Column(name = "billing_period", nullable = false)
  private String billingPeriod;

  @Column(name = "s3_object_key", nullable = false)
  private String s3ObjectKey;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  public PdfReportEntity() {}

  @PrePersist
  void ensureId() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (generatedAt == null) {
      generatedAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public DatasetEntity getDataset() {
    return dataset;
  }

  public void setDataset(DatasetEntity dataset) {
    this.dataset = dataset;
  }

  public AppUserEntity getUser() {
    return user;
  }

  public void setUser(AppUserEntity user) {
    this.user = user;
  }

  public String getBillingPeriod() {
    return billingPeriod;
  }

  public void setBillingPeriod(String billingPeriod) {
    this.billingPeriod = billingPeriod;
  }

  public String getS3ObjectKey() {
    return s3ObjectKey;
  }

  public void setS3ObjectKey(String s3ObjectKey) {
    this.s3ObjectKey = s3ObjectKey;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(Long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}
