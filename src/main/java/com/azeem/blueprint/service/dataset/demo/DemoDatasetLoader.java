/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.dataset.demo;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.repository.DatasetRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
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
  private final AppUserService appUserService;
  private final UUID DUMMY_DATA_DATASET_ID = new UUID(0L, 0L);
  private static final UUID GUEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  public DemoDatasetLoader(
      BillingIngestionService billingIngestionService,
      BillingRecordRepository billingRecordRepository,
      DatasetRepository datasetRepository,
      AppUserService appUserService) {
    this.billingIngestionService = billingIngestionService;
    this.billingRecordRepository = billingRecordRepository;
    this.datasetRepository = datasetRepository;
    this.appUserService = appUserService;
  }

  public synchronized void loadDemoData() {
    if (isLoaded()) {
      markDatasetReady();
      log.info("Demo data already loaded, cannot load again.");
      return;
    }

    DatasetEntity demo = ensureDatasetRowExists();
    demo.setStatus("LOADING");
    datasetRepository.save(demo);

    ClassPathResource resource = new ClassPathResource("dummy-data.csv");
    try (InputStream is = resource.getInputStream()) {
      log.info("Loading demo data from: {}", resource.getFilename());
      billingIngestionService.ingestData(DUMMY_DATA_DATASET_ID, is);
      markDatasetReady();
    } catch (IOException e) {
      markDatasetFailed();
      log.error("Demo data ingestion failed: {}", e.getMessage());
      throw new IllegalStateException("Demo data ingestion failed", e);
    } catch (RuntimeException e) {
      markDatasetFailed();
      log.error("Demo data ingestion failed: {}", e.getMessage(), e);
      throw e;
    }
  }

  private DatasetEntity ensureDatasetRowExists() {
    return datasetRepository
        .findById(DUMMY_DATA_DATASET_ID)
        .map(this::ensureDemoDatasetOwner)
        .orElseGet(this::createDemoDataset);
  }

  private DatasetEntity createDemoDataset() {
    DatasetEntity demo = new DatasetEntity();
    demo.setId(DUMMY_DATA_DATASET_ID);
    demo.setOwnerUser(getGuestOwner());
    demo.setBillingPeriod("2026-06");
    demo.setSourceFilename("dummy-data.csv");
    demo.setS3ObjectKey("classpath:dummy-data.csv");
    demo.setStatus("LOADING");
    demo.setUploadedAt(Instant.now());
    DatasetEntity savedDemo = datasetRepository.save(demo);
    log.info("Created demo dataset row with ID: {}", DUMMY_DATA_DATASET_ID);
    return savedDemo;
  }

  private DatasetEntity ensureDemoDatasetOwner(DatasetEntity dataset) {
    if (dataset.getOwnerUser() != null) {
      return dataset;
    }

    dataset.setOwnerUser(getGuestOwner());
    DatasetEntity savedDataset = datasetRepository.save(dataset);
    log.info("Assigned guest owner {} to existing demo dataset.", GUEST_USER_ID);
    return savedDataset;
  }

  private AppUserEntity getGuestOwner() {
    return appUserService.findOrCreateGuest(GUEST_USER_ID);
  }

  private void markDatasetReady() {
    datasetRepository
        .findById(DUMMY_DATA_DATASET_ID)
        .ifPresent(
            dataset -> {
              dataset.setStatus("READY");
              datasetRepository.save(dataset);
            });
  }

  private void markDatasetFailed() {
    datasetRepository
        .findById(DUMMY_DATA_DATASET_ID)
        .ifPresent(
            dataset -> {
              dataset.setStatus("FAILED");
              datasetRepository.save(dataset);
            });
  }

  private boolean isLoaded() {
    return billingRecordRepository.existsByDatasetIdAndBillingPeriod(
        DUMMY_DATA_DATASET_ID, "2026-06");
  }
}
