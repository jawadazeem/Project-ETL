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
import com.azeem.blueprint.model.billing.CloudProvider;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.repository.AlarmRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  @Mock private AlarmBillingQueryService alarmBillingQueryService;
  @Mock private AlarmDetectionService alarmDetectionService;
  @Mock private AlarmMapper alarmMapper;
  @Mock private BillingRecordMapper billingMapper;
  @Mock private NotificationClient notificationClient;

  @InjectMocks private AlarmService service;

  private BillingRecordEntity billingRecordEntity() {
    return new BillingRecordEntity();
  }

  private BillingRecordEntity billingRecordEntity(
      String resourceId, String serviceName, double totalCharge) {
    BillingRecordEntity entity = new BillingRecordEntity();
    entity.setResourceId(resourceId);
    entity.setServiceName(serviceName);
    entity.setTotalCharge(totalCharge);
    return entity;
  }

  private Alarm alarm(UUID businessKey) {
    return new Alarm(
        null,
        DATASET_ID,
        businessKey,
        AlarmScope.PROVIDER,
        "2026-01",
        null,
        AlarmSeverity.HIGH,
        "test",
        Instant.now(),
        null,
        null,
        CloudProvider.AWS);
  }

  private void stubAggregateQueries() {
    when(alarmBillingQueryService.getProviderTotals(eq(DATASET_ID), eq("2026-01")))
        .thenReturn(Map.of());
    when(alarmBillingQueryService.getAccountTotal(eq(DATASET_ID), eq("2026-01"))).thenReturn(0.0);
  }

  private void stubDetectionReturnsNothing() {
    when(alarmDetectionService.detectProviderAlarms(eq(DATASET_ID), anyMap(), eq("2026-01")))
        .thenReturn(List.of());
    when(alarmDetectionService.detectAccountAlarm(eq(DATASET_ID), anyDouble(), eq("2026-01")))
        .thenReturn(Optional.empty());
    when(alarmDetectionService.detectResourceAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of());
  }

  @Test
  void shouldDetectAndPersistAlarms() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of());
    stubAggregateQueries();
    when(alarmDetectionService.detectProviderAlarms(eq(DATASET_ID), anyMap(), eq("2026-01")))
        .thenReturn(List.of());
    when(alarmDetectionService.detectAccountAlarm(eq(DATASET_ID), anyDouble(), eq("2026-01")))
        .thenReturn(Optional.empty());
    when(alarmBillingQueryService.getResourceDetectionCandidates(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(billingMapper.mapToDomain(any())).thenReturn(mock(BillingRecord.class));
    when(alarmDetectionService.detectResourceAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of(alarm(new UUID(0L, 1L))));
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository).saveAll(anyList());
  }

  @Test
  void shouldNotPersistWhenNoAlarmsDetected() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of());
    stubAggregateQueries();
    stubDetectionReturnsNothing();
    when(alarmBillingQueryService.getResourceDetectionCandidates(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(Page.empty());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository, never()).saveAll(any());
  }

  @Test
  void shouldNotPersistWhenAllDetectedAlarmsAlreadyExist() {
    UUID key = new UUID(0L, 1L);
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of(key));
    stubAggregateQueries();
    when(alarmDetectionService.detectProviderAlarms(eq(DATASET_ID), anyMap(), eq("2026-01")))
        .thenReturn(List.of(alarm(key)));
    when(alarmDetectionService.detectAccountAlarm(eq(DATASET_ID), anyDouble(), eq("2026-01")))
        .thenReturn(Optional.empty());
    when(alarmBillingQueryService.getResourceDetectionCandidates(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(Page.empty());
    when(alarmDetectionService.detectResourceAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository, never()).saveAll(any());
  }

  @Test
  void shouldPersistOnlyNewAlarmsWhenSomeAlreadyExist() {
    UUID existing = new UUID(0L, 1L);
    UUID newKey = new UUID(0L, 2L);
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(List.of(existing));
    stubAggregateQueries();
    when(alarmDetectionService.detectProviderAlarms(eq(DATASET_ID), anyMap(), eq("2026-01")))
        .thenReturn(List.of(alarm(existing), alarm(newKey)));
    when(alarmDetectionService.detectAccountAlarm(eq(DATASET_ID), anyDouble(), eq("2026-01")))
        .thenReturn(Optional.empty());
    when(alarmBillingQueryService.getResourceDetectionCandidates(
            eq(DATASET_ID), anyString(), any()))
        .thenReturn(Page.empty());
    when(alarmDetectionService.detectResourceAlarms(eq(DATASET_ID), anyList(), eq("2026-01")))
        .thenReturn(List.of());
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository).saveAll(anyList());
  }

  @Test
  void shouldUseAggregateQueriesForProviderAndAccountAlarms() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    stubAggregateQueries();
    stubDetectionReturnsNothing();
    when(alarmBillingQueryService.getResourceDetectionCandidates(any(), anyString(), any()))
        .thenReturn(Page.empty());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmBillingQueryService).getProviderTotals(DATASET_ID, "2026-01");
    verify(alarmBillingQueryService).getAccountTotal(DATASET_ID, "2026-01");
    verify(alarmDetectionService).detectProviderAlarms(eq(DATASET_ID), anyMap(), eq("2026-01"));
    verify(alarmDetectionService).detectAccountAlarm(eq(DATASET_ID), anyDouble(), eq("2026-01"));
  }

  @Test
  void shouldMapBillingRecordsToDomainBeforeResourceDetection() {
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    stubAggregateQueries();
    when(alarmDetectionService.detectProviderAlarms(any(), anyMap(), anyString()))
        .thenReturn(List.of());
    when(alarmDetectionService.detectAccountAlarm(any(), anyDouble(), anyString()))
        .thenReturn(Optional.empty());
    when(alarmBillingQueryService.getResourceDetectionCandidates(any(), anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(billingRecordEntity())));
    when(billingMapper.mapToDomain(any())).thenReturn(mock(BillingRecord.class));
    when(alarmDetectionService.detectResourceAlarms(any(), anyList(), anyString()))
        .thenReturn(List.of());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(billingMapper).mapToDomain(any());
  }

  @Test
  void shouldConvertDetectedAlarmsToEntitiesBeforeSaving() {
    UUID key = new UUID(0L, 1L);
    when(alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(any(), anyString()))
        .thenReturn(List.of());
    stubAggregateQueries();
    when(alarmDetectionService.detectProviderAlarms(any(), anyMap(), anyString()))
        .thenReturn(List.of(alarm(key)));
    when(alarmDetectionService.detectAccountAlarm(any(), anyDouble(), anyString()))
        .thenReturn(Optional.empty());
    when(alarmBillingQueryService.getResourceDetectionCandidates(any(), anyString(), any()))
        .thenReturn(Page.empty());
    when(alarmDetectionService.detectResourceAlarms(any(), anyList(), anyString()))
        .thenReturn(List.of());
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
  void shouldReturnProviderAlarms() {
    when(alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
            DATASET_ID, "2026-01", AlarmScope.PROVIDER))
        .thenReturn(List.of(new AlarmEntity()));
    when(alarmMapper.mapToDomain(any())).thenReturn(alarm(new UUID(0L, 1L)));

    assertFalse(service.getProviderAlarmsInDataset(DATASET_ID, "2026-01").isEmpty());
  }

  @Test
  void shouldReturnResourceAlarms() {
    when(alarmRepository.findByDatasetIdAndBillingPeriodAndAlarmScope(
            DATASET_ID, "2026-01", AlarmScope.RESOURCE))
        .thenReturn(List.of(new AlarmEntity()));
    when(alarmMapper.mapToDomain(any())).thenReturn(alarm(new UUID(0L, 1L)));

    assertFalse(service.getResourceAlarmsInDataset(DATASET_ID, "2026-01").isEmpty());
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
    stubAggregateQueries();
    stubDetectionReturnsNothing();
    when(alarmBillingQueryService.getResourceDetectionCandidates(any(), anyString(), any()))
        .thenReturn(Page.empty());

    service.detectAndPersistAlarmsForDataset(DATASET_ID, "2026-01");

    verify(alarmRepository, never()).saveAll(any());
  }

  @Test
  void shouldDeleteProviderAlarmWhenNewLimitNoLongerRequiresIt() {
    AlarmThresholdPreference oldPreference = preference(1000, 1000, 2000, 5000, 500000, 750000);
    AlarmThresholdPreference newPreference = preference(2000, 1000, 2000, 5000, 500000, 750000);
    when(alarmBillingQueryService.getProviderTotals(DATASET_ID, "2026-01"))
        .thenReturn(Map.of("AWS", 1500.0));

    service.recompute(DATASET_ID, "2026-01", oldPreference, newPreference);

    verify(alarmRepository)
        .deleteByDatasetIdAndBillingPeriodAndAlarmScopeAndCloudProvider(
            DATASET_ID, "2026-01", AlarmScope.PROVIDER, CloudProvider.AWS);
    verify(alarmRepository, never()).save(any());
  }

  @Test
  void shouldCreateProviderAlarmWhenNewLimitRequiresIt() {
    AlarmThresholdPreference oldPreference = preference(2000, 1000, 2000, 5000, 500000, 750000);
    AlarmThresholdPreference newPreference = preference(1000, 1000, 2000, 5000, 500000, 750000);
    when(alarmBillingQueryService.getProviderTotals(DATASET_ID, "2026-01"))
        .thenReturn(Map.of("AWS", 1500.0));
    when(alarmRepository.existsByDatasetIdAndBillingPeriodAndBusinessKey(
            eq(DATASET_ID), eq("2026-01"), any(UUID.class)))
        .thenReturn(false);
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.recompute(DATASET_ID, "2026-01", oldPreference, newPreference);

    verify(alarmRepository).save(any(AlarmEntity.class));
  }

  @Test
  void shouldDeleteAndCreateResourceAlarmWhenSeverityChanges() {
    AlarmThresholdPreference oldPreference = preference(25000, 1000, 2000, 5000, 500000, 750000);
    AlarmThresholdPreference newPreference = preference(25000, 1000, 2000, 6000, 500000, 750000);
    when(alarmBillingQueryService.getResourceRecomputeCandidates(DATASET_ID, "2026-01", 1000, 6000))
        .thenReturn(List.of(billingRecordEntity("vm-1", "Compute", 5500)));
    when(alarmRepository.existsByDatasetIdAndBillingPeriodAndBusinessKey(
            eq(DATASET_ID), eq("2026-01"), any(UUID.class)))
        .thenReturn(false);
    when(alarmMapper.mapToEntity(any())).thenReturn(new AlarmEntity());

    service.recompute(DATASET_ID, "2026-01", oldPreference, newPreference);

    verify(alarmRepository)
        .deleteByDatasetIdAndBillingPeriodAndAlarmScopeAndResourceIdAndAlarmSeverity(
            DATASET_ID, "2026-01", AlarmScope.RESOURCE, "vm-1", AlarmSeverity.HIGH);
    verify(alarmRepository).save(any(AlarmEntity.class));
  }

  private AlarmThresholdPreference preference(
      double providerLimit,
      double individualLow,
      double individualMedium,
      double individualHigh,
      double accountLow,
      double accountHigh) {
    return new AlarmThresholdPreference(
        null,
        null,
        new AlarmThresholdPreference.Provider(providerLimit),
        new AlarmThresholdPreference.Individual(individualLow, individualMedium, individualHigh),
        new AlarmThresholdPreference.Account(accountLow, accountHigh));
  }
}
