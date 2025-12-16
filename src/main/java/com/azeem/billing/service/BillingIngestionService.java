package com.azeem.billing.service;

import com.azeem.billing.entity.BillingRecordEntity;
import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.repository.BillingRecordRepository;
import com.azeem.billing.util.BillingFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.azeem.billing.etl.CsvBillingReader;
import com.azeem.billing.etl.BillingRecordAssembler;
import com.azeem.billing.mapper.BillingRecordMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for ingesting billing files into the database.
 *
 * <p>
 * Orchestrates the write path of the ETL pipeline:
 * <ul>
 *   <li>Reads raw billing data from a CSV file</li>
 *   <li>Assembles domain {@link BillingRecord} objects</li>
 *   <li>Persists data as {@link BillingRecordEntity}</li>
 * </ul>
 *
 * <p>
 * This service is stateless and write-focused. After ingestion,
 * the database is the single source of truth.
 * </p>
 */

@Service
public class BillingIngestionService {
    private static final Logger log = LoggerFactory.getLogger(BillingIngestionService.class);
    private final BillingRecordRepository repository;
    private final BillingRecordAssembler billingRecordAssembler;

    public BillingIngestionService(BillingRecordAssembler billingRecordAssembler, BillingRecordRepository repository) {
        this.repository = repository;
        this.billingRecordAssembler = billingRecordAssembler;
    }

    public void ingestData(String filePath) {
        BillingFileReader billingFileReader = new CsvBillingReader(filePath, true);
        List<BillingRecord> domainRecords = billingRecordAssembler.assembleRecord(billingFileReader.parse());
        BillingRecordMapper mapper = new BillingRecordMapper();
        List<BillingRecordEntity> entityList = new ArrayList<>();

        for (BillingRecord domainRecord : domainRecords) {
            entityList.add(mapper.mapToEntity(domainRecord));
        }
        repository.saveAll(entityList);
        log.info("Billing data ingested successfully with {} records.", entityList.size());
    }
}
