/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.azeem.blueprint.entity.AlarmEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.Department;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("AlarmRepository Integration Tests")
class AlarmRepositoryTest {

  @Autowired private AlarmRepository alarmRepository;
  @Autowired private TestEntityManager entityManager;

  private DatasetEntity dataset;
  private final List<AlarmEntity> alarms = new ArrayList<>();

  @BeforeEach
  void setUp() {
    dataset = new DatasetEntity();
    dataset.setStatus("PENDING_INGESTION");
    dataset.setSourceFilename("test.csv");
    dataset.setUploadedAt(Instant.now());
    entityManager.persist(dataset);

    persistAlarm(
        UUID.fromString("b7a9f830-4e5a-4e2b-a8e9-40c26b9a896a"),
        AlarmScope.DEPARTMENT,
        "2026-02",
        "Department Charge Exceeded",
        AlarmSeverity.HIGH,
        "IT department Exceeds Charge Limit",
        Instant.now(),
        null,
        null,
        Department.IT);

    persistAlarm(
        UUID.fromString("46c50bdf-6029-474e-8dad-16a7bf14691b"),
        AlarmScope.INDIVIDUAL,
        "2026-01",
        "Individual Charge Exceeded",
        AlarmSeverity.LOW,
        "Slightly Exceeds Charge Limit",
        Instant.now(),
        "EMP192",
        "2028402810",
        Department.OPERATIONS);

    persistAlarm(
        UUID.fromString("c560fd65-9e49-48fa-8f5c-f479e60ce264"),
        AlarmScope.ACCOUNT,
        "2026-01",
        "Account Charge Exceeded",
        AlarmSeverity.HIGH,
        "Account Exceeds Charge Limit",
        Instant.now(),
        null,
        null,
        null);

    entityManager.flush();
  }

  private void persistAlarm(
      UUID businessKey,
      AlarmScope scope,
      String billingPeriod,
      String alarmType,
      AlarmSeverity severity,
      String explanation,
      Instant timestamp,
      String employeeId,
      String phoneNumber,
      Department dept) {
    AlarmEntity alarm = new AlarmEntity();
    alarm.setDataset(dataset);
    alarm.setBusinessKey(businessKey);
    alarm.setAlarmScope(scope);
    alarm.setBillingPeriod(billingPeriod);
    alarm.setAlarmType(alarmType);
    alarm.setAlarmSeverity(severity);
    alarm.setExplanation(explanation);
    alarm.setTimestamp(timestamp);
    alarm.setEmployeeId(employeeId);
    alarm.setPhoneNumber(phoneNumber);
    alarm.setDepartment(dept);
    entityManager.persist(alarm);
    alarms.add(alarm);
  }

  @Test
  @DisplayName("Should return true when alarm exists by id")
  void testFindExistsById() {
    UUID id = alarms.getFirst().getId();

    assertThat(alarmRepository.existsById(id)).isTrue();
    assertThat(alarmRepository.existsById(new UUID(0L, 0L))).isFalse();
  }

  @Test
  void testFindByDatasetIdAndBillingPeriod() {
    UUID datasetId = dataset.getId();

    assertThat(alarmRepository.findByDatasetIdAndBillingPeriod(datasetId, "2026-02"))
        .containsExactly(alarms.getFirst());
    assertThat(alarmRepository.findByDatasetIdAndBillingPeriod(datasetId, "2026-01")).hasSize(2);
  }

  @Test
  void testFindByDatasetIdAndBillingPeriodAndAlarmScope() {
    UUID datasetId = dataset.getId();

    assertThat(
            alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
                datasetId, "2026-01", AlarmScope.INDIVIDUAL))
        .containsExactly(alarms.get(1));
    assertThat(
            alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
                datasetId, "2026-01", AlarmScope.ACCOUNT))
        .hasSize(1);
  }

  @Test
  void testFindBusinessKeysByDatasetIdAndBillingPeriod() {
    UUID datasetId = dataset.getId();

    assertThat(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(datasetId, "2026-01"))
        .containsExactlyInAnyOrderElementsOf(
            alarms.stream()
                .filter(a -> a.getBillingPeriod().equals("2026-01"))
                .map(AlarmEntity::getBusinessKey)
                .toList());
  }
}
