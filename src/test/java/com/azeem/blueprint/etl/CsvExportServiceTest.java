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
import com.azeem.blueprint.model.billing.CloudProvider;
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
                DATASET_ID,
                "Acme Corp",
                "i-0abc123",
                "AWS",
                "2026-01",
                120.5,
                1.5,
                10000,
                45.75,
                "EC2",
                "m5.xlarge instance"),
            new BillingRecord(
                DATASET_ID,
                "Beta Inc",
                "proj-xyz",
                "GCP",
                "2026-01",
                90.0,
                0.8,
                5000,
                32.50,
                "BigQuery",
                "analytics query"));

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
                AlarmScope.PROVIDER,
                "2026-01",
                "Provider Spend Exceeded",
                AlarmSeverity.LOW,
                "AWS cloud spend exceeds charge limit",
                Instant.now(),
                null,
                null,
                CloudProvider.AWS));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    service.writeAlarms(alarms, out);
    String csv = out.toString();

    String[] lines = csv.split("\n");
    assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
    assertThat(lines[0]).contains("Scope");
    assertThat(lines[0]).contains("Severity");
    assertThat(lines[1]).contains("PROVIDER");
    assertThat(lines[1]).contains("AWS cloud spend exceeds charge limit");
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
