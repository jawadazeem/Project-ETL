/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "org_context_documents",
    indexes = {
      @Index(
          name = "idx_org_context_documents_owner_uploaded",
          columnList = "owner_user_id, uploaded_at"),
      @Index(name = "idx_org_context_documents_s3_key", columnList = "s3_object_key")
    })
public class OrgContextDocumentEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_user_id", nullable = false)
  private AppUserEntity ownerUser;

  @Column(name = "source_filename", nullable = false)
  private String sourceFilename;

  @Column(name = "s3_object_key", nullable = false, unique = true)
  private String s3ObjectKey;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "indexed_at")
  private Instant indexedAt;

  @Column(name = "error_message")
  private String errorMessage;

  @PrePersist
  void applyDefaults() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (uploadedAt == null) {
      uploadedAt = Instant.now();
    }
    if (status == null || status.isBlank()) {
      status = "UPLOADED";
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public AppUserEntity getOwnerUser() {
    return ownerUser;
  }

  public void setOwnerUser(AppUserEntity ownerUser) {
    this.ownerUser = ownerUser;
  }

  public String getSourceFilename() {
    return sourceFilename;
  }

  public void setSourceFilename(String sourceFilename) {
    this.sourceFilename = sourceFilename;
  }

  public String getS3ObjectKey() {
    return s3ObjectKey;
  }

  public void setS3ObjectKey(String s3ObjectKey) {
    this.s3ObjectKey = s3ObjectKey;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(Instant uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getIndexedAt() {
    return indexedAt;
  }

  public void setIndexedAt(Instant indexedAt) {
    this.indexedAt = indexedAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
