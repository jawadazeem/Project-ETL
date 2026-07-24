/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cloud_connections")
public class CloudConnectionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "owner_user_id")
  private AppUserEntity ownerUser;

  private String provider;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "bucket_name")
  private String bucketName;

  private String region;

  @Column(name = "encrypted_credentials")
  private String encryptedCredentials;

  private String status;

  @Column(name = "poll_frequency")
  private String pollFrequency;

  @Column(name = "last_polled_at")
  private Instant lastPolledAt;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

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

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getEncryptedCredentials() {
    return encryptedCredentials;
  }

  public void setEncryptedCredentials(String encryptedCredentials) {
    this.encryptedCredentials = encryptedCredentials;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPollFrequency() {
    return pollFrequency;
  }

  public void setPollFrequency(String pollFrequency) {
    this.pollFrequency = pollFrequency;
  }

  public Instant getLastPolledAt() {
    return lastPolledAt;
  }

  public void setLastPolledAt(Instant lastPolledAt) {
    this.lastPolledAt = lastPolledAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
