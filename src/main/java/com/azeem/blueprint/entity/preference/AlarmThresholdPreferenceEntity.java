/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.entity.preference;

import com.azeem.blueprint.entity.AppUserEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "alarm_threshold_preferences",
    uniqueConstraints = @UniqueConstraint(columnNames = "owner_user_id"))
public class AlarmThresholdPreferenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_user_id")
  private AppUserEntity ownerUser;

  @Column(name = "provider_monthly_limit", nullable = false)
  private double providerMonthlyLimit;

  @Column(name = "individual_low", nullable = false)
  private double individualLow;

  @Column(name = "individual_medium", nullable = false)
  private double individualMedium;

  @Column(name = "individual_high", nullable = false)
  private double individualHigh;

  @Column(name = "account_low", nullable = false)
  private double accountLow;

  @Column(name = "account_high", nullable = false)
  private double accountHigh;

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

  public double getProviderMonthlyLimit() {
    return providerMonthlyLimit;
  }

  public void setProviderMonthlyLimit(double providerMonthlyLimit) {
    this.providerMonthlyLimit = providerMonthlyLimit;
  }

  public double getIndividualLow() {
    return individualLow;
  }

  public void setIndividualLow(double individualLow) {
    this.individualLow = individualLow;
  }

  public double getIndividualMedium() {
    return individualMedium;
  }

  public void setIndividualMedium(double individualMedium) {
    this.individualMedium = individualMedium;
  }

  public double getIndividualHigh() {
    return individualHigh;
  }

  public void setIndividualHigh(double individualHigh) {
    this.individualHigh = individualHigh;
  }

  public double getAccountLow() {
    return accountLow;
  }

  public void setAccountLow(double accountLow) {
    this.accountLow = accountLow;
  }

  public double getAccountHigh() {
    return accountHigh;
  }

  public void setAccountHigh(double accountHigh) {
    this.accountHigh = accountHigh;
  }
}
