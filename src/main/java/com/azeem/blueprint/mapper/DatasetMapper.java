/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Mapper class to convert between Dataset domain model and DatasetEntity database entity. */
@Component
public class DatasetMapper {
  private final AppUserRepository appUserRepository;

  public DatasetMapper(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }

  public DatasetEntity mapToEntity(Dataset dataset) {
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setId(dataset.id());
    datasetEntity.setOwnerUser(getAppUserEntityById(dataset));
    datasetEntity.setBillingPeriod(dataset.billingPeriod());
    datasetEntity.setSourceFilename(dataset.sourceFilename());
    datasetEntity.setS3ObjectKey(dataset.s3ObjectKey());
    datasetEntity.setUploadedAt(dataset.uploadedAt());
    datasetEntity.setStatus(dataset.status());
    return datasetEntity;
  }

  public Dataset mapToDomain(DatasetEntity datasetEntity) {
    UUID ownerUserId =
        datasetEntity.getOwnerUser() != null ? datasetEntity.getOwnerUser().getId() : null;
    return new Dataset(
        datasetEntity.getId(),
        ownerUserId,
        datasetEntity.getBillingPeriod(),
        datasetEntity.getSourceFilename(),
        datasetEntity.getS3ObjectKey(),
        datasetEntity.getUploadedAt(),
        datasetEntity.getStatus());
  }

  private AppUserEntity getAppUserEntityById(Dataset dataset) {
    return appUserRepository.getReferenceById(dataset.ownerUserId());
  }
}
