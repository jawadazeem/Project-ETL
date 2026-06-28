/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DataJpaTest
@DisplayName("BillingRecordRepository Integration Tests")
class BillingRecordRepositoryTest {

  @Autowired private BillingRecordRepository repository;
  @Autowired private TestEntityManager entityManager;

  private DatasetEntity dataset;

  @BeforeEach
  void setUp() {
    dataset = new DatasetEntity();
    dataset.setStatus("PENDING_INGESTION");
    dataset.setSourceFilename("test.csv");
    dataset.setUploadedAt(Instant.now());
    entityManager.persist(dataset);

    persistRecord(
        "AWS",
        "Sherwood Williams",
        "i-0abc001",
        "2026-01",
        120.0,
        5.5,
        45,
        75.50,
        "EC2",
        "m5.large instance");
    persistRecord(
        "GCP",
        "Scott Savran",
        "proj-xyz-002",
        "2026-01",
        300.0,
        12.2,
        10,
        110.25,
        "BigQuery",
        "analytics query");
    persistRecord(
        "AWS",
        "Abdel Ebrahim",
        "i-0abc003",
        "2026-02",
        50.0,
        1.0,
        100,
        45.00,
        "S3",
        "storage bucket");

    entityManager.flush();
  }

  private void persistRecord(
      String cloudProvider,
      String name,
      String resourceId,
      String period,
      double computeHours,
      double storageGbUsed,
      long apiRequests,
      double charge,
      String serviceName,
      String description) {
    BillingRecordEntity record = new BillingRecordEntity();
    record.setDataset(dataset);
    record.setCloudProvider(cloudProvider);
    record.setAccountName(name);
    record.setResourceId(resourceId);
    record.setBillingPeriod(period);
    record.setComputeHours(computeHours);
    record.setStorageGbUsed(storageGbUsed);
    record.setApiRequests(apiRequests);
    record.setTotalCharge(charge);
    record.setServiceName(serviceName);
    record.setDescription(description);
    entityManager.persist(record);
  }

  @Test
  @DisplayName("Should return distinct billing periods for a dataset")
  void testFindBillingPeriodByDatasetId() {
    UUID datasetId = dataset.getId();
    Pageable pageable = PageRequest.of(0, 10, Sort.by("billingPeriod"));

    Page<String> periods = repository.findBillingPeriodByDatasetId(datasetId, pageable);

    assertThat(periods).isNotNull();
    assertThat(periods.getContent()).hasSize(2);
    assertThat(periods.getContent()).containsExactly("2026-01", "2026-02");
  }

  @Test
  @DisplayName("Should return distinct billing periods as a non-paged list")
  void testFindDistinctBillingPeriodByDatasetId() {
    UUID datasetId = dataset.getId();

    List<String> periods = repository.findDistinctBillingPeriodByDatasetId(datasetId);

    assertThat(periods).containsExactly("2026-01", "2026-02");
  }

  @Test
  @DisplayName("Should return true when billing period exists for a dataset")
  void testExistsByDatasetIdAndBillingPeriod() {
    UUID datasetId = dataset.getId();

    assertThat(repository.existsByDatasetIdAndBillingPeriod(datasetId, "2026-01")).isTrue();
    assertThat(repository.existsByDatasetIdAndBillingPeriod(datasetId, "1999-12")).isFalse();
  }

  @Test
  @DisplayName("Should find providers ignoring case sensitivity")
  void testFindByDatasetIdAndCloudProviderIgnoreCase() {
    UUID datasetId = dataset.getId();

    Page<BillingRecordEntity> result =
        repository.findByDatasetIdAndCloudProviderIgnoreCase(
            datasetId, "aws", PageRequest.of(0, 5));

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().getFirst().getCloudProvider()).isEqualTo("AWS");
  }

  @Test
  @DisplayName("Should return distinct cloud providers for a dataset")
  void testFindDistinctCloudProvidersByDatasetId() {
    UUID datasetId = dataset.getId();

    // Add a duplicate provider record
    persistRecord(
        "AWS",
        "Sherwood Williams",
        "i-0abc001",
        "2026-01",
        120.0,
        5.5,
        45,
        75.50,
        "EC2",
        "m5.large instance");
    entityManager.flush();

    List<String> providers = repository.findDistinctCloudProvidersByDatasetId(datasetId);

    assertThat(providers).hasSize(2);
    assertThat(providers).containsExactly("AWS", "GCP");
  }
}
