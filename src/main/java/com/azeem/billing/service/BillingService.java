package com.azeem.billing.service;

import com.azeem.billing.etl.BillParser;
import com.azeem.billing.etl.SummaryBuilder;
import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service component that orchestrates the billing ETL workflow.
 * <p>
 * This class:
 * <ul>
 *   <li>Uses {@link BillParser} to load and cache billing records from the CSV source.</li>
 *   <li>Provides access to all parsed {@link BillingRecord} objects.</li>
 *   <li>Builds a {@link BillingSummary} with aggregated analytics using {@link SummaryBuilder}.</li>
 * </ul>
 * Data is loaded lazily on first access and then reused for subsequent calls.
 */

@Service
public class BillingService {
    private final BillParser parser;
    private boolean isLoaded = false;

    public BillingService(BillParser parser) {
        this.parser = parser;
    }

    public void loadData() {
        parser.load();
        isLoaded = true;
    }

    // Ensure data is loaded before any operation (lazy loading)
    private void ensureLoaded() {
        if (!isLoaded) {
            loadData();
        }
    }

    public List<BillingRecord> getAllRecords() {
        ensureLoaded();
        return parser.getRecords();
    }

    public BillingSummary generateSummary() {
        ensureLoaded();
        List<BillingRecord> records = parser.getRecords();
        SummaryBuilder builder = new SummaryBuilder(records);
        return builder.build();
    }
}
