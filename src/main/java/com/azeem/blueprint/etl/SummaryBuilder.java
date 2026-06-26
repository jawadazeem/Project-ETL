/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SummaryBuilder is responsible for generating aggregated billing analytics from a collection of
 * BillingRecord instances.
 *
 * <p>This class performs the "Transform" step of the ETL process: it takes raw parsed records and
 * computes higher-level metrics such as:
 *
 * <ul>
 *   <li>Total number of billing records
 *   <li>Sum of all charges across records
 *   <li>The record with the highest total charge
 *   <li>Total charges grouped by department
 * </ul>
 *
 * <p>SummaryBuilder does not handle parsing, file I/O, serialization, or presentation concerns. It
 * strictly converts input data into a BillingSummary object for consumption by service and
 * controller layers.
 */
public class SummaryBuilder {
  private static final Logger log = LoggerFactory.getLogger(SummaryBuilder.class);

  private final List<BillingRecord> records;

  public SummaryBuilder(List<BillingRecord> records) {
    this.records = records;
  }

  public BillingSummary build() {
    BillingSummary summary = new BillingSummary();
    summary.setTotalRecords(calculateTotalRecords());
    summary.setTotalCharges(calculateTotalCharges());
    summary.setHighestChargeRecord(findHighestChargeRecord());
    summary.setChargesByProvider(calculateChargesByProvider());
    summary.setAverageCharge(averageCharge());

    log.info("The summary has been built successfully with {} records.", summary.getTotalRecords());
    return summary;
  }

  private int calculateTotalRecords() {
    return records.size();
  }

  private double calculateTotalCharges() {
    double totalCharge = 0.00;

    for (BillingRecord record : records) {
      totalCharge += record.totalCharge();
    }

    return Math.round(totalCharge * 100) / 100.00;
  }

  private BillingRecord findHighestChargeRecord() {
    // Defensive: when no records are present, return null rather than throwing
    if (records == null || records.isEmpty()) {
      return null;
    }

    BillingRecord currHighestChargeRecord = records.getFirst();
    for (BillingRecord record : records) {
      if (currHighestChargeRecord.totalCharge() < record.totalCharge()) {
        currHighestChargeRecord = record;
      }
    }

    return currHighestChargeRecord;
  }

  private double averageCharge() {
    if (records.isEmpty()) {
      return 0.0;
    }
    return Math.round((calculateTotalCharges() / records.size()) * 100) / 100.0;
  }

  private Map<String, Double> calculateChargesByProvider() {
    Map<String, Double> totals = new HashMap<>();

    for (BillingRecord record : records) {
      String provider = record.cloudProvider();
      double charge = record.totalCharge();
      totals.put(provider, totals.getOrDefault(provider, 0.0) + charge);
    }

    return totals;
  }
}
