package com.azeem.billing.service;

import com.azeem.billing.etl.BillParser;
import com.azeem.billing.etl.SummaryBuilder;
import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillParser parser;
    private boolean isLoaded = false;

    public BillingService(BillParser parser) {
        this.parser = parser;
    }

    public void loadData() {
        parser.load();
        isLoaded = true;
        log.info("Billing data loaded successfully with {} records.", parser.getRecords().size());
    }

    // Ensure data is loaded before any operation (lazy loading)
    private void ensureLoaded() {
        if (!isLoaded) {
            loadData();
        }
    }

    public List<BillingRecord> getAllRecords() {
        ensureLoaded();
        log.info("Retrieved all billing records, count: {}.", parser.getRecords().size());
        return parser.getRecords();
    }

    public List<BillingRecord> getRecordsByState(String state) {
        return getAllRecords().stream().filter(r -> r.state().equalsIgnoreCase(state)).toList();
    }

    public List<BillingRecord> getTopNRecords(int n) {
        return getAllRecords().stream().sorted((a, b) -> Double.compare(b.totalCharge(), a.totalCharge())).limit(n).toList();
    }

    public BillingSummary generateSummary() {
        ensureLoaded();
        List<BillingRecord> records = parser.getRecords();
        SummaryBuilder builder = new SummaryBuilder(records);
        log.info("A billing summary is being generated for {} records.", records.size());
        return builder.build();
    }
}
