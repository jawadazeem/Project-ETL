/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.opencsv.CSVWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/** Writes billing records and alarms to an OutputStream as CSV. */
@Component
public class CsvExportService {

  private static final String[] RECORD_HEADER = {
    "Account Name",
    "Resource ID",
    "Cloud Provider",
    "Billing Period",
    "Compute Hours",
    "Storage GB Used",
    "API Requests",
    "Total Charge",
    "Service Name",
    "Description"
  };

  private static final String[] ALARM_HEADER = {
    "Scope", "Type", "Severity", "Billing Period", "Explanation"
  };

  public void writeRecords(List<BillingRecord> records, OutputStream out) {
    try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      writer.writeNext(RECORD_HEADER);
      for (BillingRecord r : records) {
        writer.writeNext(
            new String[] {
              r.accountName(),
              r.resourceId(),
              r.cloudProvider(),
              r.billingPeriod(),
              String.valueOf(r.computeHours()),
              String.valueOf(r.storageGbUsed()),
              String.valueOf(r.apiRequests()),
              String.valueOf(r.totalCharge()),
              r.serviceName(),
              r.description()
            });
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to write billing records CSV", e);
    }
  }

  public void writeAlarms(List<Alarm> alarms, OutputStream out) {
    try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      writer.writeNext(ALARM_HEADER);
      for (Alarm a : alarms) {
        writer.writeNext(
            new String[] {
              a.alarmScope().name(),
              a.alarmType(),
              a.alarmSeverity().name(),
              a.billingPeriod(),
              a.explanation()
            });
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to write alarms CSV", e);
    }
  }
}
