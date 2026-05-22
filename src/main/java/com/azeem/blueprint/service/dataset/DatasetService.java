/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.dataset;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.exception.infra.DatasetNotFoundException;
import com.azeem.blueprint.mapper.DatasetMapper;
import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.repository.DatasetRepository;
import com.azeem.blueprint.service.AppUser.AppUserService;
import com.azeem.blueprint.service.billing.BillingS3Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetService {
  private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

  private final DatasetRepository datasetRepository;
  private final BillingRecordRepository billingRecordRepository;
  private final BillingS3Service s3Service;
  private final DatasetMapper datasetMapper;
  private final AppUserService appUserService;

  public DatasetService(
      DatasetRepository datasetRepository,
      BillingRecordRepository billingRecordRepository,
      BillingS3Service s3Service,
      DatasetMapper datasetMapper,
      AppUserService appUserService) {
    this.datasetRepository = datasetRepository;
    this.billingRecordRepository = billingRecordRepository;
    this.s3Service = s3Service;
    this.datasetMapper = datasetMapper;
    this.appUserService = appUserService;
  }

  /**
   * Orchestrates the initialization of a Dataset track record and streams the multipart payload
   * securely directly out to the S3 infrastructure.
   */
  @Transactional
  public Dataset initializeAndUploadDataset(String externalUserId, MultipartFile file) {
    log.info("Initializing new dataset upload for user: {}", externalUserId);

    UUID userId = UUID.fromString(externalUserId);
    AppUserEntity ownerUser = appUserService.findOrCreateGuest(userId);

    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setOwnerUser(ownerUser);
    datasetEntity.setSourceFilename(file.getOriginalFilename());
    datasetEntity.setUploadedAt(Instant.now());
    datasetEntity.setStatus("PENDING_INGESTION");

    DatasetEntity savedEntity = datasetRepository.save(datasetEntity);
    Dataset domainModel = datasetMapper.mapToDomain(savedEntity);

    // Path layout: bucket/ownerUserId/datasetId/YYYY-MM.csv
    String targetBucket = "telecom-billing";
    s3Service.uploadUserFile(targetBucket, domainModel, file);
    String s3Key =
        "%s/%s/%s"
            .formatted(domainModel.ownerUserId(), domainModel.id(), file.getOriginalFilename());
    savedEntity.setS3ObjectKey(s3Key);
    datasetRepository.save(savedEntity);

    log.info("Dataset tracking initialized successfully. UUID: {}", savedEntity.getId());
    return datasetMapper.mapToDomain(savedEntity);
  }

  public List<Dataset> listDatasets(UUID ownerUserId) {
    return datasetRepository.findByOwnerUserIdOrOwnerUserIsNull(ownerUserId).stream()
        .map(datasetMapper::mapToDomain)
        .toList();
  }

  public Dataset getDataset(UUID datasetId) {
    DatasetEntity entity =
        datasetRepository
            .findById(datasetId)
            .orElseThrow(() -> new DatasetNotFoundException(datasetId.toString()));
    return datasetMapper.mapToDomain(entity);
  }

  /** Cleans out targeted tracking elements securely under a dataset boundary. */
  @Transactional
  public void deleteRecordsByPeriodInDataset(UUID datasetId, String billingPeriod) {
    log.info("Purging billing details for period: {} inside dataset: {}", billingPeriod, datasetId);

    // Verify context exists first
    if (!datasetRepository.existsById(datasetId)) {
      throw new DatasetNotFoundException(datasetId.toString());
    }

    int deletedCount =
        billingRecordRepository.deleteByDatasetIdAndBillingPeriod(datasetId, billingPeriod);
    log.info("Successfully dropped {} orphaned billing records.", deletedCount);
  }
}
