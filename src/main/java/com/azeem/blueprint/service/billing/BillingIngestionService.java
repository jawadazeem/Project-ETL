/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.billing;

import com.azeem.blueprint.config.BillingReaderConfig;
import com.azeem.blueprint.etl.BillingRecordAssembler;
import com.azeem.blueprint.etl.CsvBillingReader;
import com.azeem.blueprint.etl.JdbcBillingBatchWriter;
import com.azeem.blueprint.exception.infra.S3SqsPipelineIngestionException;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.IngestionResult;
import com.azeem.blueprint.service.alarm.AlarmService;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Service responsible for ingesting billing files into the database.
 *
 * <p>Orchestrates the write path of the ETL pipeline:
 *
 * <ul>
 *   <li>Reads raw billing data from a CSV file
 *   <li>Assembles domain {@link BillingRecord} objects
 *   <li>Persists data via JDBC batch inserts
 * </ul>
 *
 * <p>This service is stateless and write-focused. After ingestion, the database is the single
 * source of truth.
 */
@Service
@Validated
public class BillingIngestionService {
  private static final Logger log = LoggerFactory.getLogger(BillingIngestionService.class);
  private final BillingRecordAssembler billingRecordAssembler;
  private final JdbcBillingBatchWriter batchWriter;
  private final BillingReaderConfig billingReaderConfig;
  private final AlarmService alarmService;

  public BillingIngestionService(
      BillingRecordAssembler billingRecordAssembler,
      JdbcBillingBatchWriter batchWriter,
      BillingReaderConfig billingReaderConfig,
      AlarmService alarmService) {
    this.billingRecordAssembler = billingRecordAssembler;
    this.batchWriter = batchWriter;
    this.billingReaderConfig = billingReaderConfig;
    this.alarmService = alarmService;
  }

  @Transactional
  @CacheEvict(
      value = {"billingSummaries", "billingPeriods", "providers", "alarms"},
      allEntries = true)
  public IngestionResult ingestData(@NotNull UUID datasetId, @NotNull InputStream inputStream) {
    int successCount = 0;
    int failureCount = 0;
    StringBuilder errorBuffer = new StringBuilder();
    errorBuffer.append("Raw Row,Error Message\n");
    boolean firstRow = true;
    String billingPeriod = "unknown";

    try (CsvBillingReader reader =
        new CsvBillingReader(inputStream, billingReaderConfig.hasHeader())) {
      List<BillingRecord> batch = new ArrayList<>();
      String[] row;

      while ((row = reader.parseNextRow()) != null) {
        try {
          BillingRecord domain = billingRecordAssembler.assembleRecord(row, datasetId);
          batch.add(domain);

          if (firstRow) {
            billingPeriod = domain.billingPeriod();
            firstRow = false;
          }

          if (batch.size() >= billingReaderConfig.getBatchSize()) {
            batchWriter.writeBatch(batch);
            successCount += batch.size();
            batch.clear();
          }
        } catch (Exception e) {
          failureCount++;
          log.error(
              "Resilience Triggered: Skipping corrupted row in period {}. Error: {}",
              billingPeriod,
              e.getMessage());

          // Capture the exact line that failed
          String rawRow = String.join(",", row);
          errorBuffer.append(rawRow).append(",").append(e.getMessage()).append("\n");
        }
      }

      if (!batch.isEmpty()) {
        try {
          batchWriter.writeBatch(batch);
          successCount += batch.size();
        } catch (Exception e) {
          failureCount += batch.size();
          log.error("Final batch failed at database level.");
        }
      }

      alarmService.detectAndPersistAlarmsForDataset(datasetId, billingPeriod);
      log.info(
          "Ingestion Complete for {}: {} Success, {} Failures",
          billingPeriod,
          successCount,
          failureCount);

    } catch (Exception e) {
      log.error("System-Level Failure: The S3 stream or Reader encountered a fatal error.", e);
      throw new S3SqsPipelineIngestionException("Fatal pipeline ingestion termination", e);
    }

    return new IngestionResult(
        datasetId, billingPeriod, successCount, failureCount, errorBuffer.toString());
  }
}
