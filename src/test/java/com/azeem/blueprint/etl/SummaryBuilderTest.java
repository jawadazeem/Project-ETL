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
            DATASET_ID, "Acme", "i-001", "AWS", "2026-01", 100, 1.5, 10, 45.50, "EC2", "desc"));
    records.add(
        new BillingRecord(
            DATASET_ID, "Beta", "proj-002", "GCP", "2026-01", 50, 0.5, 5, 10.00, "BigQuery",
            "desc"));
    records.add(
        new BillingRecord(
            DATASET_ID, "Acme", "i-003", "AWS", "2026-01", 10, 0.1, 1, 5.25, "S3", "desc"));

    BillingSummary summary = new SummaryBuilder(records).build();

    assertNotNull(summary);
    assertEquals(3, summary.getTotalRecords());
    assertEquals(60.75, summary.getTotalCharges(), 1e-9);
    assertNotNull(summary.getHighestChargeRecord());
    assertEquals(45.50, summary.getHighestChargeRecord().totalCharge(), 1e-9);
    assertEquals(20.25, summary.getAverageCharge(), 1e-9);

    Map<String, Double> byProvider = summary.getChargesByProvider();
    assertEquals(2, byProvider.size());
    assertEquals(50.75, byProvider.get("AWS"), 1e-9);
    assertEquals(10.00, byProvider.get("GCP"), 1e-9);
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
    assertTrue(summary.getChargesByProvider().isEmpty());
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
          new BillingRecord(
              DATASET_ID, "A", "i-001", "AWS", "2026-01", 1, 0.0, 0, 100.00, "EC2", "desc"));
      records.add(
          new BillingRecord(
              DATASET_ID, "B", "proj-002", "GCP", "2026-01", 1, 0.0, 0, 100.00, "BigQuery",
              "desc"));

      BillingSummary summary = new SummaryBuilder(records).build();

      assertNotNull(summary.getHighestChargeRecord());
      assertEquals("i-001", summary.getHighestChargeRecord().resourceId());
    }

    @Test
    @DisplayName("aggregate charges by provider with single record per provider")
    void chargesByProvider_singleRecordEach() {
      LinkedList<BillingRecord> records = new LinkedList<>();
      records.add(
          new BillingRecord(
              DATASET_ID, "A", "i-001", "AWS", "2026-01", 1, 0.0, 0, 1.00, "EC2", "desc"));
      records.add(
          new BillingRecord(
              DATASET_ID, "B", "vm-002", "AZURE", "2026-01", 1, 0.0, 0, 2.50, "Virtual Machines",
              "desc"));

      BillingSummary summary = new SummaryBuilder(records).build();

      Map<String, Double> byProvider = summary.getChargesByProvider();
      assertEquals(2, byProvider.size());
      assertEquals(1.00, byProvider.get("AWS"), 1e-9);
      assertEquals(2.50, byProvider.get("AZURE"), 1e-9);
    }
  }
}
