/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.alarm;

import static com.azeem.blueprint.model.alarm.AlarmSeverity.LOW;

import com.azeem.blueprint.model.billing.CloudProvider;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Alarm DTO
 *
 * <p>An Alarm object that represents an alarm for either the entire account, a cloud provider, or
 * an individual resource.
 */
public record Alarm(
    UUID id,
    UUID datasetId,
    UUID businessKey,
    AlarmScope alarmScope,
    String billingPeriod,
    String alarmType,
    AlarmSeverity alarmSeverity,
    String explanation,
    Instant timestamp,
    @Nullable String resourceId,
    @Nullable String serviceName,
    @Nullable CloudProvider cloudProvider) {

  public static Alarm resource(
      UUID datasetId,
      String billingPeriod,
      AlarmSeverity severity,
      String message,
      String resourceId,
      String serviceName) {
    return new Alarm(
        null,
        datasetId,
        generateBusinessKey(
            datasetId,
            billingPeriod,
            AlarmScope.RESOURCE.toString(),
            severity.toString(),
            resourceId,
            ""),
        AlarmScope.RESOURCE,
        billingPeriod,
        "Resource Charge Limit Exceeded",
        severity,
        message,
        Instant.now(),
        resourceId,
        serviceName,
        null);
  }

  public static Alarm provider(UUID datasetId, String billingPeriod, CloudProvider cloudProvider) {
    return new Alarm(
        null,
        datasetId,
        generateBusinessKey(
            datasetId,
            billingPeriod,
            AlarmScope.PROVIDER.toString(),
            AlarmSeverity.LOW.toString(),
            "",
            cloudProvider.toString()),
        AlarmScope.PROVIDER,
        billingPeriod,
        "Provider Spend Exceeded",
        LOW,
        cloudProvider + " cloud spend exceeds charge limit",
        Instant.now(),
        null,
        null,
        cloudProvider);
  }

  public static Alarm accountLow(UUID datasetId, String billingPeriod) {
    return new Alarm(
        null,
        datasetId,
        generateBusinessKey(
            datasetId,
            billingPeriod,
            AlarmScope.ACCOUNT.toString(),
            AlarmSeverity.LOW.toString(),
            "",
            ""),
        AlarmScope.ACCOUNT,
        billingPeriod,
        "Total Account Budget Exceeded: LOW",
        AlarmSeverity.LOW,
        "Your account's cloud spend has slightly exceeded its monthly budget.",
        Instant.now(),
        null,
        null,
        null);
  }

  public static Alarm accountHigh(UUID datasetId, String billingPeriod) {
    return new Alarm(
        null,
        datasetId,
        generateBusinessKey(
            datasetId,
            billingPeriod,
            AlarmScope.ACCOUNT.toString(),
            AlarmSeverity.HIGH.toString(),
            "",
            ""),
        AlarmScope.ACCOUNT,
        billingPeriod,
        "Total Account Budget Exceeded: HIGH",
        AlarmSeverity.HIGH,
        "Your account's cloud spend has significantly exceeded its monthly budget.",
        Instant.now(),
        null,
        null,
        null);
  }

  /** Generates a deterministic business key for deduplication */
  private static UUID generateBusinessKey(
      UUID datasetId,
      String billingPeriod,
      String alarmScope,
      String alarmSeverity,
      String resourceId,
      String cloudProvider) {

    String fingerprint =
        datasetId + billingPeriod + alarmScope + alarmSeverity + resourceId + cloudProvider;

    return UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8));
  }
}
