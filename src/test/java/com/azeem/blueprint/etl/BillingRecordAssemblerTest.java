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
      "Acme Corp", "E12345", "Engineering", "555-0100", "2026-01", "120", "1.5", "10", "45.75"
    };

    BillingRecord record = assembler.assembleRecord(row, DATASET_ID);

    assertAll(
        () -> assertEquals(DATASET_ID, record.datasetId()),
        () -> assertEquals("Acme Corp", record.accountName()),
        () -> assertEquals("E12345", record.employeeId()),
        () -> assertEquals("Engineering", record.department()),
        () -> assertEquals("555-0100", record.phoneNumber()),
        () -> assertEquals("2026-01", record.billingPeriod()),
        () -> assertEquals(120, record.minutesUsed()),
        () -> assertEquals(1.5, record.dataGbUsed(), 1e-9),
        () -> assertEquals(10, record.smsCount()),
        () -> assertEquals(45.75, record.totalCharge(), 1e-9));
  }

  @Test
  @DisplayName("assembleRecords should convert multiple rows into a List of BillingRecord")
  void assembleRecords_multipleRows_returnsList() {
    List<String[]> rows =
        List.of(
            new String[] {"A", "id1", "DeptA", "111", "2026-01", "10", "0.1", "1", "2.5"},
            new String[] {"B", "id2", "DeptB", "222", "2026-02", "20", "0.2", "2", "5.0"});

    List<BillingRecord> records = assembler.assembleRecords(rows, DATASET_ID);

    assertEquals(2, records.size());
    assertEquals(DATASET_ID, records.get(0).datasetId());
    assertEquals("id1", records.get(0).employeeId());
    assertEquals("DeptB", records.get(1).department());
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
        "Acme", "E1", "D", "000", "2026-01", "not-a-number", "0.0", "0", "0.0"
      };
      assertThrows(
          NumberFormatException.class, () -> assembler.assembleRecord(badNumbers, DATASET_ID));
    }
  }
}
