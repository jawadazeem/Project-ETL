/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.billing;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Billing Record DTO
 *
 * <p>Represents a single cloud billing line item.
 */
public record BillingRecord(
    UUID datasetId,
    String accountName,
    String resourceId,
    String cloudProvider,
    String billingPeriod,
    double computeHours,
    double storageGbUsed,
    long apiRequests,
    double totalCharge,
    String serviceName,
    String description) {

  @NotNull
  @Override
  public String toString() {
    return datasetId
        + ", "
        + accountName
        + ", "
        + resourceId
        + ", "
        + cloudProvider
        + ", "
        + billingPeriod
        + ", "
        + computeHours
        + ", "
        + storageGbUsed
        + ", "
        + apiRequests
        + ", "
        + totalCharge
        + ", "
        + serviceName
        + ", "
        + description;
  }
}
