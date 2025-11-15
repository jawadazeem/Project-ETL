package com.azeem.billing.service;

import com.azeem.billing.etl.BillParser;
import com.azeem.billing.etl.SummaryBuilder;
import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
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
