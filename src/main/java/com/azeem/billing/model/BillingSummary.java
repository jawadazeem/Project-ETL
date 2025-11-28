package com.azeem.billing.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the aggregated billing analytics produced by the ETL pipeline.
 *
 * <p>This class serves as the data model (output schema) It contains high-level metrics derived from a
 * collection of {@link BillingRecord} objects, including:
 *
 * <ul>
 *     <li>Total number of billing records processed</li>
 *     <li>Total accumulated charges across all records</li>
 *     <li>The record with the highest individual total charge</li>
 *     <li>Aggregated charges grouped by state</li>
 * </ul>
 *
 * <p>This class contains no business logic. It is a simple container intended to be
 * constructed and populated by the analytics layer (SummaryBuilder)
 */

public class BillingSummary {

    private int totalRecords;
    private double totalCharges;
    private BillingRecord highestChargeRecord;
    private double averageCharge;
    private Map<String, Double> chargesByState;

    public BillingSummary() {
        this.totalRecords = 0;
        this.totalCharges = 0.0;
        this.highestChargeRecord = null;
        this.averageCharge = 0;
        this.chargesByState = new HashMap<>();
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

    public Map<String, Double> getChargesByState() {
        return chargesByState;
    }

    public void setChargesByState(Map<String, Double> chargesByState) {
        this.chargesByState = chargesByState;
    }

    @Override
    public String toString() {
        return "BillingSummary{" +
                "totalRecords=" + totalRecords +
                ", totalCharges=" + totalCharges +
                '}';
    }

}
