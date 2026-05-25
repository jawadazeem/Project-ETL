/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import static org.assertj.core.api.Assertions.assertThat;

import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.BillingRecord;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CsvExportServiceTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final CsvExportService service = new CsvExportService();

  @Test
  @DisplayName("writeRecords produces CSV with header and data rows")
  void shouldWriteRecordsCsv() {
    List<BillingRecord> records =
        List.of(
            new BillingRecord(
                DATASET_ID, "Acme Corp", "E001", "Engineering", "555-0100", "2026-01", 120, 1.5,
                10, 45.75),
            new BillingRecord(
                DATASET_ID, "Beta Inc", "E002", "Finance", "555-0200", "2026-01", 90, 0.8, 5,
                32.50));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.writeRecords(records, out);
    String csv = out.toString();

    String[] lines = csv.split("\n");
    assertThat(lines).hasSizeGreaterThanOrEqualTo(3);
    assertThat(lines[0]).contains("Account Name");
    assertThat(lines[0]).contains("Total Charge");
    assertThat(lines[1]).contains("Acme Corp");
    assertThat(lines[2]).contains("Beta Inc");
  }

  @Test
  @DisplayName("writeRecords with empty list produces header only")
  void shouldWriteHeaderOnlyForEmptyRecords() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.writeRecords(Collections.emptyList(), out);
    String csv = out.toString();

    String[] lines = csv.trim().split("\n");
    assertThat(lines).hasSize(1);
    assertThat(lines[0]).contains("Account Name");
  }

  @Test
  @DisplayName("writeAlarms produces CSV with header and data rows")
  void shouldWriteAlarmsCsv() {
    List<Alarm> alarms =
        List.of(
            new Alarm(
                UUID.randomUUID(),
                DATASET_ID,
                UUID.randomUUID(),
                AlarmScope.DEPARTMENT,
                "2026-01",
                "Department Charge Exceeded",
                AlarmSeverity.LOW,
                "Engineering department exceeds limit",
                Instant.now(),
                null,
                null,
                null));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.writeAlarms(alarms, out);
    String csv = out.toString();

    String[] lines = csv.split("\n");
    assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
    assertThat(lines[0]).contains("Scope");
    assertThat(lines[0]).contains("Severity");
    assertThat(lines[1]).contains("DEPARTMENT");
    assertThat(lines[1]).contains("Engineering department exceeds limit");
  }

  @Test
  @DisplayName("writeAlarms with empty list produces header only")
  void shouldWriteHeaderOnlyForEmptyAlarms() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.writeAlarms(Collections.emptyList(), out);
    String csv = out.toString();

    String[] lines = csv.trim().split("\n");
    assertThat(lines).hasSize(1);
    assertThat(lines[0]).contains("Scope");
  }
}
