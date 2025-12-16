package com.azeem.billing.service;

import com.azeem.billing.entity.BillingRecordEntity;
import com.azeem.billing.etl.SummaryBuilder;
import com.azeem.billing.mapper.BillingRecordMapper;
import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import com.azeem.billing.repository.BillingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/**
 * Stateless service providing read-only billing operations.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Query billing data from the database</li>
 *   <li>Map entities to domain models</li>
 *   <li>Generate billing summaries</li>
 * </ul>
 *
 * <p>
 * This service assumes billing data has already been ingested and persisted.
 * All state lives in the database, making the service safe for concurrent use.
 * </p>
 */

@Service
public class BillingService {
    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRecordRepository repository;
    private final BillingRecordMapper mapper = new BillingRecordMapper();

    public BillingService(BillingRecordRepository repository) {
        this.repository = repository;
    }

    public List<String> getAvailableBillingPeriods() {
        return repository.findAllBillingPeriods(); // No mapper for object of BillingPeriod type
    }

    public List<BillingRecord> getRecordsByPeriod(String billingPeriod) {
        return repository.findByBillingPeriod(billingPeriod).stream()
                .map(mapper::mapToDomain)
                .toList();
    }

    public List<BillingRecord> getAllRecords() {
        List<BillingRecord> records = repository.findAll().stream()
                .map(mapper::mapToDomain)
                .toList();
        log.info("Retrieved all billing records, count: {}.", records.size());
        return records;
    }

    public List<BillingRecord> getRecordsByDepartment(String department) {
        List<BillingRecord> records = getAllRecords().stream()
                .filter(r -> r.department()
                        .equalsIgnoreCase(department)).toList();
        log.info("Retrieved {}'s records, count: {}.", department, records.size());
        return records;
    }

    public List<BillingRecord> getTopNRecords(int n) {
        List<BillingRecord> records = getAllRecords().stream()
                .sorted((a, b) ->
                        Double.compare(b.totalCharge(), a.totalCharge())).limit(n).toList();
        log.info("Retrieved top {} records, count: {}.", n, records.size());
        return records;
    }

    public BillingSummary generateSummaryForPeriod(String billingPeriod) {
        List<BillingRecord> records = getRecordsByPeriod(billingPeriod);
        return new SummaryBuilder(records).build();
    }

    public BillingSummary generateSummary() {
        List<BillingRecord> records = getAllRecords();
        SummaryBuilder builder = new SummaryBuilder(records);
        log.info("A billing summary is being generated for {} records.", records.size());
        return builder.build();
    }
}
