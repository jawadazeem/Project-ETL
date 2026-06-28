/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import com.azeem.blueprint.client.NotificationClient;
import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.mapper.AlarmMapper;
import com.azeem.blueprint.mapper.BillingRecordMapper;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.repository.AlarmRepository;
import com.azeem.blueprint.repository.BillingRecordRepository;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmService {

  private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

  AlarmRepository alarmRepository;
  BillingRecordRepository billingRecordRepository;
  AlarmMapper alarmMapper;
  BillingRecordMapper billingMapper;
  AlarmDetectionService alarmDetectionService;
  NotificationClient notificationClient;

  public AlarmService(
      AlarmRepository alarmRepository,
      BillingRecordRepository billingRecordRepository,
      AlarmDetectionService alarmDetectionService,
      AlarmMapper alarmMapper,
      BillingRecordMapper billingRecordMapper,
      NotificationClient notificationClient) {
    this.alarmRepository = alarmRepository;
    this.billingRecordRepository = billingRecordRepository;
    this.alarmDetectionService = alarmDetectionService;
    this.billingMapper = billingRecordMapper;
    this.alarmMapper = alarmMapper;
    this.notificationClient = notificationClient;
  }

  /**
   * Detects alarms from billing records and persists only new ones for the given billing period.
   *
   * <p>Provider and account-level alarms use SQL aggregates computed over the full dataset-period,
   * so they are accurate regardless of dataset size. Resource alarms (per-record threshold checks)
   * are still processed in chunks to limit memory usage.
   *
   * <p>Newly persisted alarms are forwarded to the notification service on a best-effort basis.
   */
  @Transactional
  public void detectAndPersistAlarmsForDataset(UUID datasetId, String billingPeriod) {
    Set<UUID> existingKeys =
        new HashSet<>(
            alarmRepository.findBusinessKeysByDatasetIdAndBillingPeriod(datasetId, billingPeriod));

    List<Alarm> allNewAlarms = new ArrayList<>();

    // Provider alarms — computed from SQL aggregates over the full dataset-period
    Map<String, Double> providerTotals = buildProviderTotals(datasetId, billingPeriod);
    List<Alarm> providerAlarms =
        alarmDetectionService.detectProviderAlarms(datasetId, providerTotals, billingPeriod);
    persistNewAlarms(providerAlarms, existingKeys, allNewAlarms);

    // Account-level alarm — computed from SQL aggregate over the full dataset-period
    double grandTotal =
        billingRecordRepository.sumTotalChargeByDatasetIdAndBillingPeriod(datasetId, billingPeriod);
    alarmDetectionService
        .detectAccountAlarm(datasetId, grandTotal, billingPeriod)
        .ifPresent(alarm -> persistNewAlarms(List.of(alarm), existingKeys, allNewAlarms));

    // Resource alarms — chunked, since each record is checked independently
    int page = 0;
    int chunkSize = 1000;
    boolean hasMore = true;
    while (hasMore) {
      Page<BillingRecordEntity> chunk =
          billingRecordRepository.findByDatasetIdAndBillingPeriod(
              datasetId, billingPeriod, PageRequest.of(page++, chunkSize));

      List<BillingRecord> chunkList = chunk.stream().map(billingMapper::mapToDomain).toList();
      List<Alarm> resourceAlarms =
          alarmDetectionService.detectResourceAlarms(datasetId, chunkList, billingPeriod);
      persistNewAlarms(resourceAlarms, existingKeys, allNewAlarms);

      hasMore = chunk.hasNext();
    }

    if (!allNewAlarms.isEmpty()) {
      notifyQuietly(allNewAlarms);
    }
  }

  private Map<String, Double> buildProviderTotals(UUID datasetId, String billingPeriod) {
    Map<String, Double> totals = new HashMap<>();
    for (Object[] row :
        billingRecordRepository.sumTotalChargeGroupedByCloudProvider(datasetId, billingPeriod)) {
      String provider = (String) row[0];
      Double total = (Double) row[1];
      if (provider != null && total != null) {
        totals.put(provider, total);
      }
    }
    return totals;
  }

  private void persistNewAlarms(
      List<Alarm> detected, Set<UUID> existingKeys, List<Alarm> allNewAlarms) {
    if (detected.isEmpty()) return;
    List<Alarm> newAlarms =
        detected.stream().filter(a -> !existingKeys.contains(a.businessKey())).toList();
    if (!newAlarms.isEmpty()) {
      alarmRepository.saveAll(newAlarms.stream().map(alarmMapper::mapToEntity).toList());
      newAlarms.forEach(a -> existingKeys.add(a.businessKey()));
      allNewAlarms.addAll(newAlarms);
    }
  }

  /** Retrieves all alarms for a given billing period. */
  @Cacheable(value = "alarms", key = "#datasetId + '-' + #billingPeriod")
  public List<Alarm> getAllAlarmsInDataset(UUID datasetId, String billingPeriod) {
    return alarmRepository.findByDatasetIdAndBillingPeriod(datasetId, billingPeriod).stream()
        .map(alarmMapper::mapToDomain)
        .toList();
  }

  /** Retrieves alarms scoped to cloud providers for a billing period. */
  @Cacheable(value = "alarms", key = "#datasetId + '-' + #billingPeriod + '-provider'")
  public List<Alarm> getProviderAlarmsInDataset(UUID datasetId, String billingPeriod) {
    return alarmRepository
        .findByDatasetIdAndBillingPeriodAndAlarmScope(
            datasetId, billingPeriod, AlarmScope.PROVIDER)
        .stream()
        .map(alarmMapper::mapToDomain)
        .toList();
  }

  /** Retrieves alarms scoped to individual resources for a billing period. */
  @Cacheable(value = "alarms", key = "#datasetId + '-' + #billingPeriod + '-resource'")
  public List<Alarm> getResourceAlarmsInDataset(UUID datasetId, String billingPeriod) {
    return alarmRepository
        .findByDatasetIdAndBillingPeriodAndAlarmScope(
            datasetId, billingPeriod, AlarmScope.RESOURCE)
        .stream()
        .map(alarmMapper::mapToDomain)
        .toList();
  }

  /** Retrieves account-level alarms for a billing period. */
  @Cacheable(value = "alarms", key = "#datasetId + '-' + #billingPeriod + '-account'")
  public List<Alarm> getAccountAlarm(UUID datasetId, String billingPeriod) {
    return alarmRepository
        .findByDatasetIdAndBillingPeriodAndAlarmScope(datasetId, billingPeriod, AlarmScope.ACCOUNT)
        .stream()
        .map(alarmMapper::mapToDomain)
        .toList();
  }

  private void notifyQuietly(List<Alarm> alarms) {
    try {
      notificationClient.sendAlarmNotifications(alarms);
    } catch (Exception e) {
      log.warn(
          "Alarm notifications could not be delivered to notification service: {}", e.getMessage());
    }
  }
}
