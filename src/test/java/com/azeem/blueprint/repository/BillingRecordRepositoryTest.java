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
        "Engineering", "Sherwood Williams", "EMP001", "7035550123", "2026-01", 120, 5.5, 45, 75.50);
    persistRecord(
        "Operations", "Scott Savran", "EMP002", "7035550456", "2026-01", 300, 12.2, 10, 110.25);
    persistRecord(
        "Engineering", "Abdel Ebrahim", "EMP003", "7035550789", "2026-02", 50, 1.0, 100, 45.00);

    entityManager.flush();
  }

  private void persistRecord(
      String dept,
      String name,
      String empId,
      String phone,
      String period,
      int mins,
      double data,
      int sms,
      double charge) {
    BillingRecordEntity record = new BillingRecordEntity();
    record.setDataset(dataset);
    record.setDepartment(dept);
    record.setAccountName(name);
    record.setEmployeeId(empId);
    record.setPhoneNumber(phone);
    record.setBillingPeriod(period);
    record.setMinutesUsed(mins);
    record.setDataGbUsed(data);
    record.setSmsCount(sms);
    record.setTotalCharge(charge);
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
  @DisplayName("Should find departments ignoring case sensitivity")
  void testFindByDatasetIdAndDepartmentIgnoreCase() {
    UUID datasetId = dataset.getId();

    Page<BillingRecordEntity> result =
        repository.findByDatasetIdAndDepartmentIgnoreCase(
            datasetId, "engineering", PageRequest.of(0, 5));

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().getFirst().getDepartment()).isEqualTo("Engineering");
  }

  @Test
  @DisplayName("Should return distinct departments for a dataset")
  void testFindDistinctDepartmentsByDatasetId() {
    UUID datasetId = dataset.getId();

    // Add a duplicate department record
    persistRecord(
        "Engineering", "Sherwood Williams", "EMP001", "7035550123", "2026-01", 120, 5.5, 45, 75.50);
    entityManager.flush();

    List<String> departments = repository.findDistinctDepartmentsByDatasetId(datasetId);

    assertThat(departments).hasSize(2);
    assertThat(departments).containsExactly("Engineering", "Operations");
  }
}
