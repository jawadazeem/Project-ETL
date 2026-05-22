/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import static org.junit.jupiter.api.Assertions.*;

import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SummaryBuilderTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  @DisplayName("build() produces correct summary for a small dataset")
  void build_happyPath_producesCorrectSummary() {
    LinkedList<BillingRecord> records = new LinkedList<>();
    records.add(
        new BillingRecord(
            DATASET_ID, "Acme", "E1", "Engineering", "555-0001", "2026-01", 100, 1.5, 10, 45.50));
    records.add(
        new BillingRecord(
            DATASET_ID, "Beta", "E2", "Sales", "555-0002", "2026-01", 50, 0.5, 5, 10.00));
    records.add(
        new BillingRecord(
            DATASET_ID, "Acme", "E3", "Engineering", "555-0003", "2026-01", 10, 0.1, 1, 5.25));

    BillingSummary summary = new SummaryBuilder(records).build();

    assertNotNull(summary);
    assertEquals(3, summary.getTotalRecords());
    assertEquals(60.75, summary.getTotalCharges(), 1e-9);
    assertNotNull(summary.getHighestChargeRecord());
    assertEquals(45.50, summary.getHighestChargeRecord().totalCharge(), 1e-9);
    assertEquals(20.25, summary.getAverageCharge(), 1e-9);

    Map<String, Double> byDept = summary.getChargesByDepartment();
    assertEquals(2, byDept.size());
    assertEquals(50.75, byDept.get("Engineering"), 1e-9);
    assertEquals(10.00, byDept.get("Sales"), 1e-9);
  }

  @Test
  @DisplayName("build() on empty list returns zeroed summary")
  void build_emptyList_returnsZeroedSummary() {
    LinkedList<BillingRecord> records = new LinkedList<>();

    BillingSummary summary = new SummaryBuilder(records).build();

    assertNotNull(summary);
    assertEquals(0, summary.getTotalRecords());
    assertEquals(0.0, summary.getTotalCharges(), 1e-9);
    assertEquals(0.0, summary.getAverageCharge(), 1e-9);
    assertTrue(summary.getChargesByDepartment().isEmpty());
    assertNull(summary.getHighestChargeRecord());
  }

  @Nested
  @DisplayName("Aggregation edge-cases")
  class AggregationEdgeCases {

    @Test
    @DisplayName("handles ties for highest charge by selecting the first encountered")
    void tiesForHighestCharge_selectsFirstEncountered() {
      LinkedList<BillingRecord> records = new LinkedList<>();
      records.add(
          new BillingRecord(DATASET_ID, "A", "id1", "D1", "p1", "2026-01", 1, 0.0, 0, 100.00));
      records.add(
          new BillingRecord(DATASET_ID, "B", "id2", "D2", "p2", "2026-01", 1, 0.0, 0, 100.00));

      BillingSummary summary = new SummaryBuilder(records).build();

      assertNotNull(summary.getHighestChargeRecord());
      assertEquals("id1", summary.getHighestChargeRecord().employeeId());
    }

    @Test
    @DisplayName("aggregate charges by department with single record per department")
    void chargesByDepartment_singleRecordEach() {
      LinkedList<BillingRecord> records = new LinkedList<>();
      records.add(
          new BillingRecord(DATASET_ID, "A", "id1", "D1", "p1", "2026-01", 1, 0.0, 0, 1.00));
      records.add(
          new BillingRecord(DATASET_ID, "B", "id2", "D2", "p2", "2026-01", 1, 0.0, 0, 2.50));

      BillingSummary summary = new SummaryBuilder(records).build();

      Map<String, Double> byDept = summary.getChargesByDepartment();
      assertEquals(2, byDept.size());
      assertEquals(1.00, byDept.get("D1"), 1e-9);
      assertEquals(2.50, byDept.get("D2"), 1e-9);
    }
  }
}
