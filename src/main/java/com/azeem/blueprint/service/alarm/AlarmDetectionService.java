/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.CloudProvider;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.service.dataset.DatasetService;
import com.azeem.blueprint.service.preference.AlarmPreferenceQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Input: List<BillingRecord> (scoped by billingPeriod)
 *
 * <p>Output: List<Alarm> (NOT persisted)
 *
 * <p>No repositories
 *
 * <p>No side effects
 */
@Service
public class AlarmDetectionService {
  private final AlarmPreferenceQueryService alarmPreferenceQueryService;
  private final DatasetService datasetService;

  public AlarmDetectionService(
      AlarmPreferenceQueryService alarmPreferenceQueryService, DatasetService datasetService) {
    this.alarmPreferenceQueryService = alarmPreferenceQueryService;
    this.datasetService = datasetService;
  }

  public List<Alarm> detectAlarms(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    List<Alarm> alarms = getProvidersOverLimit(datasetId, records, billingPeriod);
    alarms.addAll(getResourceChargesOverLimit(datasetId, records, billingPeriod));
    Optional<Alarm> grandTotalAlarm = getGrandTotalOverLimit(datasetId, records, billingPeriod);
    grandTotalAlarm.ifPresent(alarms::add);
    return alarms;
  }

  /** Detects resource alarms only — safe to call per-chunk since each record is independent. */
  public List<Alarm> detectResourceAlarms(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    return getResourceChargesOverLimit(datasetId, records, billingPeriod);
  }

  /** Detects provider alarms from pre-computed totals (full dataset, not chunked). */
  public List<Alarm> detectProviderAlarms(
      UUID datasetId, Map<String, Double> providerTotals, String billingPeriod) {
    AlarmThresholdPreference preference = getPreferenceFromDataset(datasetId);
    List<Alarm> alarms = new ArrayList<>();
    double providerLimit = preference.provider().monthlyLimit();

    for (Map.Entry<String, Double> entry : providerTotals.entrySet()) {
      if (entry.getValue() > providerLimit) {
        try {
          CloudProvider provider = CloudProvider.fromString(entry.getKey());
          alarms.add(Alarm.provider(datasetId, billingPeriod, provider));
        } catch (IllegalArgumentException ignored) {
          // Unknown provider name — skip
        }
      }
    }

    return alarms;
  }

  /** Detects account-level alarm from the pre-computed grand total (full dataset, not chunked). */
  public Optional<Alarm> detectAccountAlarm(
      UUID datasetId, double grandTotal, String billingPeriod) {
    AlarmThresholdPreference preference = getPreferenceFromDataset(datasetId);
    double accountLow = preference.account().low();
    double accountHigh = preference.account().high();

    if (grandTotal >= accountHigh) {
      return Optional.of(Alarm.accountHigh(datasetId, billingPeriod));
    } else if (grandTotal > accountLow) {
      return Optional.of(Alarm.accountLow(datasetId, billingPeriod));
    }
    return Optional.empty();
  }

  private List<Alarm> getProvidersOverLimit(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    AlarmThresholdPreference preference = getPreferenceFromDataset(datasetId);
    List<Alarm> alarms = new ArrayList<>();
    Map<CloudProvider, Double> totals = new HashMap<>();

    for (BillingRecord r : records) {
      if (r.cloudProvider() == null) continue;
      totals.merge(CloudProvider.fromString(r.cloudProvider()), r.totalCharge(), Double::sum);
    }

    double providerLimit = preference.provider().monthlyLimit();

    for (CloudProvider p : totals.keySet()) {
      if (totals.getOrDefault(p, 0.0) > providerLimit) {
        Alarm alarm = Alarm.provider(datasetId, billingPeriod, p);
        alarms.add(alarm);
      }
    }

    return alarms;
  }

  private List<Alarm> getResourceChargesOverLimit(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    AlarmThresholdPreference preference = getPreferenceFromDataset(datasetId);
    List<Alarm> alarms = new ArrayList<>();

    double low = preference.individual().low();
    double medium = preference.individual().medium();
    double high = preference.individual().high();

    for (BillingRecord r : records) {
      double charge = r.totalCharge();
      AlarmSeverity severity;
      String message;

      if (charge >= low && charge < medium) {
        severity = AlarmSeverity.LOW;
        message = "Exceeds Charge Limit: LOW";
      } else if (charge >= medium && charge < high) {
        severity = AlarmSeverity.MEDIUM;
        message = "Slightly exceeds Charge Limit: MEDIUM";
      } else if (charge >= high) {
        severity = AlarmSeverity.HIGH;
        message = "Significantly exceeds Charge Limit (TAKE ACTION)";
      } else {
        continue;
      }
      Alarm alarm =
          Alarm.resource(
              datasetId, billingPeriod, severity, message, r.resourceId(), r.serviceName());
      alarms.add(alarm);
    }

    return alarms;
  }

  private Optional<Alarm> getGrandTotalOverLimit(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    AlarmThresholdPreference preference = getPreferenceFromDataset(datasetId);
    double grandTotal = 0;
    double accountLow = preference.account().low();
    double accountHigh = preference.account().high();

    for (BillingRecord r : records) {
      grandTotal += r.totalCharge();
    }

    if (grandTotal >= accountHigh) {
      return Optional.of(Alarm.accountHigh(datasetId, billingPeriod));
    } else if (grandTotal > accountLow) {
      return Optional.of(Alarm.accountLow(datasetId, billingPeriod));
    }
    return Optional.empty();
  }

  private AlarmThresholdPreference getPreferenceFromDataset(UUID datasetId) {
    UUID ownerId = datasetService.getOwnerId(datasetId);
    return alarmPreferenceQueryService.getPreference(ownerId);
  }
}
