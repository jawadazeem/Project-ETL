/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.dataset.demo;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.exception.core.BillingDataIngestionException;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.repository.dataset.DatasetRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.billing.BillingIngestionService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Class used for loading demo data for demonstration purposes. Used by those who may not have a
 * properly formatted CSV file to run analytics on
 */
// TODO: Update to use S3 bucket as a source of truth for storage (just as real users would).
//  Avoid loading from classpath.
//  Would it make sense to take care of scheduling of ingestion in the Java monolith?
//  Simply calling the Python service when need be. That way, a user in CloudConnection
//  can also do poll frequency on the frontend easily? The Cloud ingestion service
//  knows very little, just connects to the buckets using what the Java gives it and
//  returns formatted file by putting it in teh proper s3 key for the user?
@Component
public class DemoDatasetLoader {
  Logger log = LoggerFactory.getLogger(DemoDatasetLoader.class);
  private final BillingIngestionService billingIngestionService;
  private final BillingRecordRepository billingRecordRepository;
  private final DatasetRepository datasetRepository;
  private final AppUserService appUserService;
  private final Map<UUID, String> DUMMY_DATA_DATASET_IDS =
      Map.of(
          new UUID(0L, 0L),
          "2026-04",
          UUID.fromString("00000000-0000-0000-0000-000000000001"),
          "2026-05",
          UUID.fromString("00000000-0000-0000-0000-000000000002"),
          "2026-06",
          UUID.fromString("00000000-0000-0000-0000-000000000003"),
          "2026-07");
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
      DUMMY_DATA_DATASET_IDS.keySet().forEach(this::markDatasetReady);
      log.info("Demo data already loaded, cannot load again.");
      return;
    }

    ensureDatasetRowsExistAndSetLoading();

    List<ClassPathResource> resources =
        List.of(
            new ClassPathResource("2026-04.csv"),
            new ClassPathResource("2026-05.csv"),
            new ClassPathResource("2026-06.csv"),
            new ClassPathResource("2026-07.csv"));
    resources.forEach(
        r -> {
          String filename = r.getFilename();
          String period = filename.substring(0, filename.length() - 4);
          Optional<UUID> key =
              DUMMY_DATA_DATASET_IDS.entrySet().stream()
                  .filter(entry -> Objects.equals(entry.getValue(), period))
                  .map(Map.Entry::getKey)
                  .findFirst();
          UUID result =
              key.orElseThrow(
                  () ->
                      new BillingDataIngestionException(
                          "Could not load demo data: invalid filename/period"));
          try (InputStream is = r.getInputStream()) {
            log.info("Loading demo data from: {}", filename);
            billingIngestionService.ingestData(result, is);
            markDatasetReady(result);
          } catch (IOException e) {
            markDatasetFailed(result);
            log.error("Demo data ingestion failed: {}", e.getMessage());
            throw new IllegalStateException("Demo data ingestion failed", e);
          } catch (RuntimeException e) {
            markDatasetFailed(result);
            log.error("Demo data ingestion failed: {}", e.getMessage(), e);
            throw e;
          }
        });
  }

  private void ensureDatasetRowsExistAndSetLoading() {
    AppUserEntity guestOwner = getGuestOwner();

    DUMMY_DATA_DATASET_IDS.forEach(
        (id, period) -> {
          datasetRepository
              .findById(id)
              .ifPresentOrElse(
                  existingEntity -> {
                    existingEntity.setStatus("LOADING");
                    if (existingEntity.getOwnerUser() == null) {
                      existingEntity.setOwnerUser(guestOwner);
                    }
                    datasetRepository.save(existingEntity);
                  },
                  () -> {
                    DatasetEntity demo = new DatasetEntity();
                    demo.setId(id);
                    demo.setOwnerUser(guestOwner);
                    demo.setBillingPeriod(period);
                    demo.setSourceFilename(period + ".csv");
                    demo.setS3ObjectKey("classpath:" + period + ".csv");
                    demo.setStatus("LOADING");
                    demo.setUploadedAt(Instant.now());
                    datasetRepository.save(demo);
                  });
        });
  }

  private AppUserEntity getGuestOwner() {
    return appUserService.findOrCreateGuest(GUEST_USER_ID);
  }

  private void markDatasetReady(UUID datasetId) {
    datasetRepository
        .findById(datasetId)
        .ifPresent(
            dataset -> {
              dataset.setStatus("READY");
              datasetRepository.save(dataset);
            });
  }

  private void markDatasetFailed(UUID datasetId) {
    datasetRepository
        .findById(datasetId)
        .ifPresent(
            dataset -> {
              dataset.setStatus("FAILED");
              datasetRepository.save(dataset);
            });
  }

  private boolean isLoaded() {
    long existingCount =
        billingRecordRepository.countByDatasetIdIn((DUMMY_DATA_DATASET_IDS.keySet()));
    return existingCount > 0;
  }
}
