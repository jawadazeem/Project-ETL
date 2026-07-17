/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.repository.dataset.DatasetRepository;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between BillingRecord domain model and BillingRecordEntity database
 * entity.
 */
@Component
public class BillingRecordMapper {
  private final DatasetRepository datasetRepository;

  public BillingRecordMapper(DatasetRepository datasetRepository) {
    this.datasetRepository = datasetRepository;
  }

  public BillingRecordEntity mapToEntity(BillingRecord record) {
    BillingRecordEntity entity = new BillingRecordEntity();
    entity.setDataset(getDatasetById(record));
    entity.setAccountName(record.accountName());
    entity.setResourceId(record.resourceId());
    entity.setCloudProvider(record.cloudProvider());
    entity.setBillingPeriod(record.billingPeriod());
    entity.setComputeHours(record.computeHours());
    entity.setStorageGbUsed(record.storageGbUsed());
    entity.setApiRequests(record.apiRequests());
    entity.setTotalCharge(record.totalCharge());
    entity.setServiceName(record.serviceName());
    entity.setDescription(record.description());
    return entity;
  }

  public BillingRecord mapToDomain(BillingRecordEntity entity) {
    return new BillingRecord(
        entity.getDataset().getId(),
        entity.getAccountName(),
        entity.getResourceId(),
        entity.getCloudProvider(),
        entity.getBillingPeriod(),
        entity.getComputeHours(),
        entity.getStorageGbUsed(),
        entity.getApiRequests(),
        entity.getTotalCharge(),
        entity.getServiceName(),
        entity.getDescription());
  }

  private DatasetEntity getDatasetById(BillingRecord record) {
    return datasetRepository.getReferenceById(record.datasetId());
  }
}
