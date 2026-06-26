/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.billing;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the aggregated billing analytics produced by the ETL pipeline.
 *
 * <p>This class serves as the data model (output schema) It contains high-level metrics derived
 * from a collection of {@link BillingRecord} objects, including:
 *
 * <ul>
 *   <li>Total number of billing records processed
 *   <li>Total accumulated charges across all records
 *   <li>The record with the highest individual total charge
 *   <li>Aggregated charges grouped by department
 * </ul>
 *
 * <p>This class contains no business logic. It is a simple container intended to be constructed and
 * populated by the analytics layer (SummaryBuilder)
 */
public class BillingSummary {

  private int totalRecords;
  private double totalCharges;
  private BillingRecord highestChargeRecord;
  private double averageCharge;
  private Map<String, Double> chargesByProvider;

  public BillingSummary() {
    this.totalRecords = 0;
    this.totalCharges = 0.0;
    this.highestChargeRecord = null;
    this.averageCharge = 0;
    this.chargesByProvider = new HashMap<>();
  }

  public int getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(int totalRecords) {
    this.totalRecords = totalRecords;
  }

  public double getTotalCharges() {
    return totalCharges;
  }

  public void setTotalCharges(double totalCharges) {
    this.totalCharges = totalCharges;
  }

  public BillingRecord getHighestChargeRecord() {
    return highestChargeRecord;
  }

  public void setHighestChargeRecord(BillingRecord highestChargeRecord) {
    this.highestChargeRecord = highestChargeRecord;
  }

  public double getAverageCharge() {
    return averageCharge;
  }

  public void setAverageCharge(double averageCharge) {
    this.averageCharge = averageCharge;
  }

  public Map<String, Double> getChargesByProvider() {
    return chargesByProvider;
  }

  public void setChargesByProvider(Map<String, Double> chargesByProvider) {
    this.chargesByProvider = chargesByProvider;
  }

  @Override
  public String toString() {
    return "BillingSummary{"
        + "totalRecords="
        + totalRecords
        + ", totalCharges="
        + totalCharges
        + '}';
  }
}
