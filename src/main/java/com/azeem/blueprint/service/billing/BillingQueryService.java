/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.billing;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.etl.SummaryBuilder;
import com.azeem.blueprint.exception.core.BillingDataNotFoundException;
import com.azeem.blueprint.exception.web.QueryLimitExceededException;
import com.azeem.blueprint.mapper.BillingRecordMapper;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.validation.BillingPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Stateless service providing billing operations. */
@Service
@Transactional(readOnly = true)
@Validated
public class BillingQueryService {
  private static final Logger log = LoggerFactory.getLogger(BillingQueryService.class);

  @Value("${billing.charges.max-top-n:100}")
  private int maxTopNLimit;

  private final BillingRecordRepository repository;
  private final BillingRecordMapper mapper;

  public BillingQueryService(BillingRecordRepository repository, BillingRecordMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public Page<String> getAvailableBillingPeriods(UUID datasetId, int page, int size) {
    Pageable periodsRequest = PageRequest.of(page, size, Sort.by("billingPeriod").descending());
    return repository.findBillingPeriodByDatasetId(datasetId, periodsRequest);
  }

  // DB-backed distinct billing periods for a given dataset (small result set expected)
  @Cacheable(value = "billingPeriods", key = "#datasetId")
  public List<String> getDistinctBillingPeriodsById(UUID datasetId) {
    return repository.findDistinctBillingPeriodByDatasetId(datasetId);
  }

  public Page<BillingRecord> getDatasetRecordsByPeriod(
      UUID datasetId, @BillingPeriod String billingPeriod, int page, int size) {
    Pageable periodRequest = PageRequest.of(page, size);

    Page<BillingRecord> records =
        repository
            .findByDatasetIdAndBillingPeriod(datasetId, billingPeriod, periodRequest)
            .map(mapper::mapToDomain);

    if (records.isEmpty()) {
      throw new BillingDataNotFoundException(
          "Could not find billing data for period: " + billingPeriod);
    }

    return records;
  }

  public BillingRecord getByResourceId(UUID datasetId, String resourceId) {
    BillingRecordEntity record =
        repository
            .findByDataset_IdAndResourceId(datasetId, resourceId)
            .orElseThrow(
                () ->
                    new BillingDataNotFoundException(
                        "Could not find billing record with resource id: " + resourceId));
    return mapper.mapToDomain(record);
  }

  public List<BillingRecord> getByCloudProvider(UUID datasetId, String cloudProvider) {
    List<BillingRecordEntity> record =
        repository.findByDataset_IdAndCloudProvider(datasetId, cloudProvider);
    return record.stream().map(mapper::mapToDomain).toList();
  }

  /**
   * Purges all billing records associated with a specific billing period strictly within the
   * boundaries of the given dataset.
   */
  public int deleteRecordsByPeriodInDataset(UUID datasetId, @BillingPeriod String billingPeriod) {
    int rowsDeleted = repository.deleteByDatasetIdAndBillingPeriod(datasetId, billingPeriod);

    if (rowsDeleted == 0) {
      throw new BillingDataNotFoundException(
          "Could not find billing data for period: " + billingPeriod);
    }

    return rowsDeleted;
  }

  public Page<BillingRecord> getAllRecordsInDataset(UUID dataset, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("cloudProvider").descending());

    Page<BillingRecord> records =
        repository.findByDatasetId(dataset, pageable).map(mapper::mapToDomain);

    log.info("Retrieved billing records page {} with size {}.", page, size);
    return records;
  }

  public Page<BillingRecord> getRecordsByProviderInDataset(
      UUID datasetId, @NotBlank String provider, int page, int size) {
    Pageable pageRequest = PageRequest.of(page, size, Sort.by("totalCharge").descending());
    Page<BillingRecordEntity> entityPage =
        repository.findByDatasetIdAndCloudProviderIgnoreCase(datasetId, provider, pageRequest);
    log.info("Found {} records in DB for provider: {}", entityPage.getTotalElements(), provider);

    return entityPage.map(mapper::mapToDomain);
  }

  public Page<BillingRecord> getRecordsByProviderInDatasetForPeriod(
      UUID datasetId,
      @NotBlank String billingPeriod,
      @NotBlank String provider,
      int page,
      int size) {
    Pageable pageRequest = PageRequest.of(page, size, Sort.by("totalCharge").descending());
    Page<BillingRecordEntity> entityPage =
        repository.findByDatasetIdAndBillingPeriodAndCloudProviderIgnoreCase(
            datasetId, billingPeriod, provider, pageRequest);
    log.info(
        "Found {} records in DB for provider: {} and period: {}",
        entityPage.getTotalElements(),
        provider,
        billingPeriod);

    return entityPage.map(mapper::mapToDomain);
  }

  @Cacheable(value = "providers", key = "#datasetId")
  public List<String> getDistinctProvidersInDataset(UUID datasetId) {
    return repository.findDistinctCloudProvidersByDatasetId(datasetId);
  }

  public Page<BillingRecord> getTopNRecordsInDataset(UUID datasetId, @Min(1) int n) {
    if (n > maxTopNLimit) {
      throw new QueryLimitExceededException(
          "Requested record count exceeds the maximum allowed limit of " + maxTopNLimit);
    }
    if (n > repository.count()) {
      throw new QueryLimitExceededException(
          "Requested record count exceeds the maximum number of records ingested");
    }

    Pageable topNRequest = PageRequest.of(0, n, Sort.by("totalCharge").descending());
    Page<BillingRecord> records =
        repository.findByDatasetId(datasetId, topNRequest).map(mapper::mapToDomain);
    log.info("Retrieved top {} records, count: {}.", n, records.getTotalElements());

    return records;
  }

  public List<BillingRecord> getAllRecordsForExport(UUID datasetId, String billingPeriod) {
    return repository.findByDatasetIdAndBillingPeriod(datasetId, billingPeriod).stream()
        .map(mapper::mapToDomain)
        .toList();
  }

  @Cacheable(value = "billingSummaries", key = "#datasetId + '-' + #billingPeriod")
  public BillingSummary generateSummaryForPeriodInDataset(
      UUID datasetId, @BillingPeriod String billingPeriod) {
    List<BillingRecord> records =
        repository.findByDatasetIdAndBillingPeriod(datasetId, billingPeriod).stream()
            .map(mapper::mapToDomain)
            .toList();

    if (records.isEmpty()) {
      throw new BillingDataNotFoundException(
          "No billing data was found for period " + billingPeriod);
    }

    return new SummaryBuilder(records).build();
  }

  @Cacheable(value = "billingSummaries", key = "#datasetId")
  public BillingSummary generateSummary(UUID datasetId) {

    int page = 0;
    int size = 1000;

    List<BillingRecord> all = new ArrayList<>();

    while (true) {

      Page<BillingRecord> recordsPage = getAllRecordsInDataset(datasetId, page, size);

      if (recordsPage.isEmpty()) {
        break;
      }

      all.addAll(recordsPage.getContent());

      if (recordsPage.isLast()) {
        break;
      }

      page++;
    }

    if (all.isEmpty()) {
      throw new BillingDataNotFoundException("No billing data was found for summary generation");
    }

    log.info("A billing summary is being generated for {} records.", all.size());

    return new SummaryBuilder(all).build();
  }
}
