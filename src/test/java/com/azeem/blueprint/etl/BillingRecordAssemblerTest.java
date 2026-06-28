/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import static org.junit.jupiter.api.Assertions.*;

import com.azeem.blueprint.model.billing.BillingRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BillingRecordAssemblerTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final BillingRecordAssembler assembler = new BillingRecordAssembler();

  @Test
  @DisplayName("assembleRecord should convert a valid row into a BillingRecord")
  void assembleRecord_validRow_createsBillingRecord() {
    String[] row = {
      "Acme Corp", "i-0abc123", "AWS", "2026-01", "120.5", "1.5", "10000", "45.75", "EC2",
      "m5.xlarge instance"
    };

    BillingRecord record = assembler.assembleRecord(row, DATASET_ID);

    assertAll(
        () -> assertEquals(DATASET_ID, record.datasetId()),
        () -> assertEquals("Acme Corp", record.accountName()),
        () -> assertEquals("i-0abc123", record.resourceId()),
        () -> assertEquals("AWS", record.cloudProvider()),
        () -> assertEquals("2026-01", record.billingPeriod()),
        () -> assertEquals(120.5, record.computeHours(), 1e-9),
        () -> assertEquals(1.5, record.storageGbUsed(), 1e-9),
        () -> assertEquals(10000, record.apiRequests()),
        () -> assertEquals(45.75, record.totalCharge(), 1e-9),
        () -> assertEquals("EC2", record.serviceName()),
        () -> assertEquals("m5.xlarge instance", record.description()));
  }

  @Test
  @DisplayName("assembleRecords should convert multiple rows into a List of BillingRecord")
  void assembleRecords_multipleRows_returnsList() {
    List<String[]> rows =
        List.of(
            new String[] {
              "A", "i-001", "AWS", "2026-01", "10", "0.1", "100", "2.5", "EC2", "test instance"
            },
            new String[] {
              "B", "proj-002", "GCP", "2026-02", "20", "0.2", "200", "5.0", "BigQuery",
              "analytics query"
            });

    List<BillingRecord> records = assembler.assembleRecords(rows, DATASET_ID);

    assertEquals(2, records.size());
    assertEquals(DATASET_ID, records.get(0).datasetId());
    assertEquals("i-001", records.get(0).resourceId());
    assertEquals("GCP", records.get(1).cloudProvider());
  }

  @Nested
  @DisplayName("Failure modes")
  class FailureModes {

    @Test
    @DisplayName("assembleRecord should throw ArrayIndexOutOfBoundsException for too few columns")
    void assembleRecord_tooFewColumns_throws() {
      String[] shortRow = {"Only", "a", "few"};
      assertThrows(
          ArrayIndexOutOfBoundsException.class,
          () -> assembler.assembleRecord(shortRow, DATASET_ID));
    }

    @Test
    @DisplayName("assembleRecord should throw NumberFormatException for invalid numeric fields")
    void assembleRecord_invalidNumber_throws() {
      String[] badNumbers = {
        "Acme", "i-001", "AWS", "2026-01", "not-a-number", "0.0", "0", "0.0", "EC2", "desc"
      };
      assertThrows(
          NumberFormatException.class, () -> assembler.assembleRecord(badNumbers, DATASET_ID));
    }
  }
}
