package com.azeem.billing.etl;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SummaryBuilder is responsible for generating aggregated billing analytics
 * from a collection of BillingRecord instances.
 *
 * <p>This class performs the "Transform" step of the ETL process:
 * it takes raw parsed records and computes higher-level metrics such as:
 * <ul>
 *     <li>Total number of billing records</li>
 *     <li>Sum of all charges across records</li>
 *     <li>The record with the highest total charge</li>
 *     <li>Total charges grouped by state</li>
 * </ul>
 *
 * <p>SummaryBuilder does not handle parsing, file I/O, serialization,
 * or presentation concerns. It strictly converts input data into a
 * BillingSummary object for consumption by service and controller layers.
 */


public class SummaryBuilder {
    private final List<BillingRecord> records;

    public SummaryBuilder(List<BillingRecord> records) {
        this.records = records;
    }

    public BillingSummary build() {
        BillingSummary summary = new BillingSummary();
        summary.setTotalRecords(calculateTotalRecords());
        summary.setTotalCharges(calculateTotalCharges());
        summary.setHighestChargeRecord(findHighestChargeRecord());
        summary.setChargesByState(calculateChargesByState());

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
        BillingRecord currHighestChargeRecord = records.get(0);

        for (BillingRecord record: records) {
            if (currHighestChargeRecord.totalCharge() < record.totalCharge()) {
                currHighestChargeRecord = record;
            }
        }

        return currHighestChargeRecord;
    }

    private Map<String, Double> calculateChargesByState() {
        Map<String, Double> totals = new HashMap<>();

        for (BillingRecord record: records) {
            String state = record.state();
            double charge = record.totalCharge();
            totals.put(state, totals.getOrDefault(state, 0.0) + charge);
        }

        return totals;
    }


}
