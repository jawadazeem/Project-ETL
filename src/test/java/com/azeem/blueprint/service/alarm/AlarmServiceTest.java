/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.client.NotificationClient;
import com.azeem.blueprint.entity.AlarmEntity;
import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.mapper.AlarmMapper;
import com.azeem.blueprint.mapper.BillingRecordMapper;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.Department;
import com.azeem.blueprint.repository.AlarmRepository;
import com.azeem.blueprint.repository.BillingRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private AlarmRepository alarmRepository;
  @Mock private BillingRecordRepository billingRecordRepository;
  @Mock private AlarmDetectionService alarmDetectionService;
  @Mock private AlarmMapper alarmMapper;
  @Mock private BillingRecordMapper billingMapper;
  @Mock private NotificationClient notificationClient;

  @InjectMocks private AlarmService service;

  private BillingRecordEntity billingRecordEntity() {
    return new BillingRecordEntity();
  }

  private Alarm alarm(UUID businessKey) {
    return new Alarm(
        null,
        DATASET_ID,
        businessKey,
        AlarmScope.DEPARTMENT,
        "2026-01",
        null,
        AlarmSeverity.HIGH,
        "test",
        Instant.now(),
        null,
        null,
        Department.IT);
  }

  @Test
  void shouldDetectAndPersistAlarms() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of());
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(billingMapper.mapToDomain(any())).thenReturn(mock(BillingRecord.class));
    when(alarmDetectionService.detectAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of(alarm(new UUID(0L, 1L))));
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository).saveAll(anyList());
  }

  @Test
  void shouldNotPersistWhenNoAlarmsDetected() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of());
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(Page.empty());
    when(alarmDetectionService.detectAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository, never()).saveAll(any());
  }

  @Test
  void shouldNotPersistWhenAllDetectedAlarmsAlreadyExist() {
    UUID key = new UUID(0L, 1L);
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of(key));
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(alarmDetectionService.detectAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of(alarm(key)));

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository, never()).saveAll(any());
  }

  @Test
  void shouldPersistOnlyNewAlarmsWhenSomeAlreadyExist() {
    UUID existing = new UUID(0L, 1L);
    UUID newKey = new UUID(0L, 2L);
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of(existing));
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(alarmDetectionService.detectAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of(alarm(existing), alarm(newKey)));
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository).saveAll(anyList());
  }

  @Test
  void shouldMapBillingRecordsToDomainBeforeDetection() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(any(), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(alarmDetectionService.detectAlarms(any(), anyList(), anyString())).thenReturn(List.of());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(billingMapper).mapToDomain(any());
  }

  @Test
  void shouldPassCorrectParamsToDetectionService() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(any(), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(alarmDetectionService.detectAlarms(any(), anyList(), anyString())).thenReturn(List.of());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmDetectionService).detectAlarms(eq(DATASET_ID), anyList(), eq("2026-01"));
  }

  @Test
  void shouldConvertDetectedAlarmsToEntitiesBeforeSaving() {
    UUID key = new UUID(0L, 1L);
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(any(), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(alarmDetectionService.detectAlarms(any(), anyList(), anyString()))
        .thenReturn(List.of(alarm(key)));
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmMapper).mapToEntity(any());
  }

  @Test
  void shouldReturnAllPersistedAlarms() {
    when(alarmRepository.findByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of(new AlarmEntity()));
    when(alarmMapper.mapToDomain(any())).thenReturn(alarm(new UUID(0L, 1L)));

    List<Alarm> result = service.getAllAlarmsInDataset(DATASET_ID, "2026-01");

    assertFalse(result.isEmpty());
  }

  @Test
  void shouldReturnDepartmentAlarms() {
    when(alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
            DATASET_ID, "2026-01", AlarmScope.DEPARTMENT))
        .thenReturn(List.of(new AlarmEntity()));
    when(alarmMapper.mapToDomain(any())).thenReturn(alarm(new UUID(0L, 1L)));

    assertFalse(service.getDepartmentAlarmsInDataset(DATASET_ID, "2026-01").isEmpty());
  }

  @Test
  void shouldReturnIndividualAlarms() {
    when(alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
            DATASET_ID, "2026-01", AlarmScope.INDIVIDUAL))
        .thenReturn(List.of(new AlarmEntity()));
    when(alarmMapper.mapToDomain(any())).thenReturn(alarm(new UUID(0L, 1L)));

    assertFalse(service.getIndividualAlarmsInDataset(DATASET_ID, "2026-01").isEmpty());
  }

  @Test
  void shouldReturnAccountAlarms() {
    when(alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
            DATASET_ID, "2026-01", AlarmScope.ACCOUNT))
        .thenReturn(List.of(new AlarmEntity()));
    when(alarmMapper.mapToDomain(any())).thenReturn(alarm(new UUID(0L, 1L)));

    assertFalse(service.getAccountAlarm(DATASET_ID, "2026-01").isEmpty());
  }

  @Test
  void shouldReturnEmptyListWhenNoAlarmsExist() {
    when(alarmRepository.findByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of());

    assertTrue(service.getAllAlarmsInDataset(DATASET_ID, "2026-01").isEmpty());
  }

  @Test
  void shouldHandleEmptyBillingRecordsGracefully() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(any(), anyString(), any()))
        .thenReturn(Page.empty());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository, never()).saveAll(any());
  }
}
