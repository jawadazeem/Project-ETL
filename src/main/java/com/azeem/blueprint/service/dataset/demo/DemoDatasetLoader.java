/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.dataset.demo;

import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.repository.DatasetRepository;
import com.azeem.blueprint.service.billing.BillingIngestionService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Class used for loading demo data for demonstration purposes. Used by those who may not have a
 * properly formatted CSV file to run analytics on
 */
@Component
public class DemoDatasetLoader {
  Logger log = LoggerFactory.getLogger(DemoDatasetLoader.class);
  private final BillingIngestionService billingIngestionService;
  private final BillingRecordRepository billingRecordRepository;
  private final DatasetRepository datasetRepository;
  private final UUID DUMMY_DATA_DATASET_ID = new UUID(0L, 0L);

  public DemoDatasetLoader(
      BillingIngestionService billingIngestionService,
      BillingRecordRepository billingRecordRepository,
      DatasetRepository datasetRepository) {
    this.billingIngestionService = billingIngestionService;
    this.billingRecordRepository = billingRecordRepository;
    this.datasetRepository = datasetRepository;
  }

  public void loadDemoData() {
    if (isLoaded()) {
      log.info("Demo data already loaded, cannot load again.");
      return;
    }

    ensureDatasetRowExists();

    ClassPathResource resource = new ClassPathResource("dummy-data.csv");
    try (InputStream is = resource.getInputStream()) {
      log.info("Loading demo data from: {}", resource.getFilename());
      billingIngestionService.ingestData(DUMMY_DATA_DATASET_ID, is);
    } catch (IOException e) {
      log.error("Demo data ingestion failed: {}", e.getMessage());
    }
  }

  private void ensureDatasetRowExists() {
    if (datasetRepository.existsById(DUMMY_DATA_DATASET_ID)) {
      return;
    }
    DatasetEntity demo = new DatasetEntity();
    demo.setId(DUMMY_DATA_DATASET_ID);
    demo.setSourceFilename("dummy-data.csv");
    demo.setStatus("READY");
    demo.setUploadedAt(Instant.now());
    datasetRepository.save(demo);
    log.info("Created demo dataset row with ID: {}", DUMMY_DATA_DATASET_ID);
  }

  private boolean isLoaded() {
    return billingRecordRepository.existsByDatasetIdAndBillingPeriod(
        DUMMY_DATA_DATASET_ID, "dummy-data");
  }
}
