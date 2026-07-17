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
import com.azeem.blueprint.repository.dataset.DatasetBillingPeriodProjection;
import com.azeem.blueprint.repository.dataset.DatasetRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.billing.BillingS3Service;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetService {
  private static final Logger log = LoggerFactory.getLogger(DatasetService.class);
  private static final UUID GUEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
    String s3Key =
        "%s/%s/%s".formatted(ownerUser.getId(), savedEntity.getId(), file.getOriginalFilename());
    savedEntity.setS3ObjectKey(s3Key);
    savedEntity = datasetRepository.save(savedEntity);

    Dataset domainModel = datasetMapper.mapToDomain(savedEntity);
    String targetBucket = "cloud-billing";

    try {
      s3Service.uploadUserFile(targetBucket, domainModel, file);
    } catch (RuntimeException e) {
      savedEntity.setStatus("FAILED");
      datasetRepository.save(savedEntity);
      throw e;
    }

    log.info("Dataset tracking initialized successfully. UUID: {}", savedEntity.getId());
    return datasetMapper.mapToDomain(savedEntity);
  }

  public List<Dataset> listDatasets(UUID ownerUserId) {
    return datasetRepository.findActiveDatasets(ownerUserId).stream()
        .map(datasetMapper::mapToDomain)
        .toList();
  }

  public List<Dataset> listArchivedDatasets(UUID ownerUserId) {
    return datasetRepository.findArchivedDatasets(ownerUserId).stream()
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

  @Transactional
  public void deleteDataset(UUID datasetId, UUID ownerUserId) {
    log.info("Deleting dataset: {} for user: {}", datasetId, ownerUserId);
    DatasetEntity entity =
        datasetRepository
            .findByIdAndOwnerUserId(datasetId, ownerUserId)
            .orElseThrow(() -> new DatasetNotFoundException(datasetId.toString()));
    datasetRepository.delete(entity);
  }

  @Transactional
  public void archiveDataset(UUID datasetId, UUID ownerUserId) {
    log.info("Archiving dataset: {} for user: {}", datasetId, ownerUserId);
    DatasetEntity entity =
        datasetRepository
            .findByIdAndOwnerUserId(datasetId, ownerUserId)
            .orElseThrow(() -> new DatasetNotFoundException(datasetId.toString()));
    entity.setArchived(true);
    datasetRepository.save(entity);
  }

  @Transactional
  public void restoreDataset(UUID datasetId, UUID ownerUserId) {
    log.info("Restoring dataset: {} for user: {}", datasetId, ownerUserId);
    DatasetEntity entity =
        datasetRepository
            .findByIdAndOwnerUserId(datasetId, ownerUserId)
            .orElseThrow(() -> new DatasetNotFoundException(datasetId.toString()));
    entity.setArchived(false);
    datasetRepository.save(entity);
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

  public UUID getOwnerId(UUID datasetId) {
    DatasetEntity dataset =
        datasetRepository
            .findById(datasetId)
            .orElseThrow(() -> new DatasetNotFoundException(datasetId.toString()));

    if (dataset.getOwnerUser() == null) {
      log.warn(
          "Dataset {} has no owner_user_id. Falling back to guest user for legacy/demo data.",
          datasetId);
      AppUserEntity guestUser = appUserService.findOrCreateGuest(GUEST_USER_ID);
      dataset.setOwnerUser(guestUser);
      datasetRepository.save(dataset);
      return GUEST_USER_ID;
    }

    return dataset.getOwnerUser().getId();
  }

  /**
   * It might seem unusual to have billing-specific retrieval logic in the DatasetService class when
   * there is already a dedicated service layer for BillingRecord DTOs. However, the billing period
   * is a field shared by both Dataset and BillingRecord. The retrieval logic belongs here because a
   * BillingRecord does not have knowledge of the associated user in the same way that a Dataset
   * does.
   */
  public Map<UUID, String> getBillingPeriods(UUID userId) {
    return datasetRepository.findBillingPeriodsByOwnerUserId(userId).stream()
        .collect(
            Collectors.toMap(
                DatasetBillingPeriodProjection::getId,
                DatasetBillingPeriodProjection::getBillingPeriod));
  }
}
