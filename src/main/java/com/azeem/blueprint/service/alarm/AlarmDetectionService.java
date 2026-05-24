/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import com.azeem.blueprint.config.AlarmConfig;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.Department;
import java.util.*;
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
  private final AlarmConfig alarmConfig;

  public AlarmDetectionService(AlarmConfig alarmConfig) {
    this.alarmConfig = alarmConfig;
  }

  public List<Alarm> detectAlarms(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    List<Alarm> alarms = getDepartmentsOverLimit(datasetId, records, billingPeriod);
    alarms.addAll(getIndividualChargesOverLimit(datasetId, records, billingPeriod));
    Optional<Alarm> grandTotalAlarm = getGrandTotalOverLimit(datasetId, records, billingPeriod);
    grandTotalAlarm.ifPresent(alarms::add);
    return alarms;
  }

  /** Detects individual alarms only — safe to call per-chunk since each record is independent. */
  public List<Alarm> detectIndividualAlarms(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    return getIndividualChargesOverLimit(datasetId, records, billingPeriod);
  }

  /** Detects department alarms from pre-computed totals (full dataset, not chunked). */
  public List<Alarm> detectDepartmentAlarms(
      UUID datasetId, Map<String, Double> departmentTotals, String billingPeriod) {
    List<Alarm> alarms = new ArrayList<>();
    double deptLimit = alarmConfig.getDepartment().getMonthlyLimit();

    for (Map.Entry<String, Double> entry : departmentTotals.entrySet()) {
      if (entry.getValue() > deptLimit) {
        try {
          Department dept = Department.fromString(entry.getKey());
          alarms.add(Alarm.department(datasetId, billingPeriod, dept));
        } catch (IllegalArgumentException ignored) {
          // Unknown department name — skip
        }
      }
    }

    return alarms;
  }

  /** Detects account-level alarm from the pre-computed grand total (full dataset, not chunked). */
  public Optional<Alarm> detectAccountAlarm(
      UUID datasetId, double grandTotal, String billingPeriod) {
    double accountLow = alarmConfig.getAccount().getLow();
    double accountHigh = alarmConfig.getAccount().getHigh();

    if (grandTotal >= accountHigh) {
      return Optional.of(Alarm.accountHigh(datasetId, billingPeriod));
    } else if (grandTotal > accountLow) {
      return Optional.of(Alarm.accountLow(datasetId, billingPeriod));
    }
    return Optional.empty();
  }

  private List<Alarm> getDepartmentsOverLimit(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    List<Alarm> alarms = new ArrayList<>();
    Map<Department, Double> totals = new HashMap<>();

    for (BillingRecord r : records) {
      if (r.department() == null) continue;
      totals.merge(Department.fromString(r.department()), r.totalCharge(), Double::sum);
    }

    double deptLimit = alarmConfig.getDepartment().getMonthlyLimit();

    for (Department d : totals.keySet()) {
      if (totals.getOrDefault(d, 0.0) > deptLimit) {
        Alarm alarm = Alarm.department(datasetId, billingPeriod, d);
        alarms.add(alarm);
      }
    }

    return alarms;
  }

  private List<Alarm> getIndividualChargesOverLimit(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    List<Alarm> alarms = new ArrayList<>();

    double low = alarmConfig.getIndividual().getLow();
    double medium = alarmConfig.getIndividual().getMedium();
    double high = alarmConfig.getIndividual().getHigh();

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
        // No alarm
        continue;
      }
      Alarm alarm =
          Alarm.individual(
              datasetId, billingPeriod, severity, message, r.employeeId(), r.phoneNumber());
      alarms.add(alarm);
    }

    return alarms;
  }

  private Optional<Alarm> getGrandTotalOverLimit(
      UUID datasetId, List<BillingRecord> records, String billingPeriod) {
    double grandTotal = 0;
    double accountLow = alarmConfig.getAccount().getLow();
    double accountHigh = alarmConfig.getAccount().getHigh();

    for (BillingRecord r : records) {
      grandTotal += r.totalCharge();
    }

    if (grandTotal > accountLow && grandTotal < accountHigh) {
      return Optional.of(Alarm.accountLow(datasetId, billingPeriod));
    } else if (grandTotal >= accountLow) {
      return Optional.of(Alarm.accountHigh(datasetId, billingPeriod));
    }
    return Optional.empty();
  }
}
