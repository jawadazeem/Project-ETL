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
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.CloudProvider;
import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.repository.AlarmRepository;
import com.azeem.blueprint.service.dataset.DatasetService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmService {
  private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

  private final AlarmRepository alarmRepository;
  private final AlarmBillingQueryService alarmBillingQueryService;
  private final AlarmMapper alarmMapper;
  private final BillingRecordMapper billingMapper;
  private final AlarmDetectionService alarmDetectionService;
  private final NotificationClient notificationClient;
  private final DatasetService datasetService;
  private final CacheManager cacheManager;

  public AlarmService(
      AlarmRepository alarmRepository,
      AlarmBillingQueryService alarmBillingQueryService,
      AlarmDetectionService alarmDetectionService,
      AlarmMapper alarmMapper,
      BillingRecordMapper billingRecordMapper,
      NotificationClient notificationClient,
      DatasetService datasetService,
      CacheManager cacheManager) {
    this.alarmRepository = alarmRepository;
    this.alarmBillingQueryService = alarmBillingQueryService;
    this.alarmDetectionService = alarmDetectionService;
    this.billingMapper = billingRecordMapper;
    this.alarmMapper = alarmMapper;
    this.notificationClient = notificationClient;
    this.datasetService = datasetService;
    this.cacheManager = cacheManager;
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
    Map<String, Double> providerTotals =
        alarmBillingQueryService.getProviderTotals(datasetId, billingPeriod);
    List<Alarm> providerAlarms =
        alarmDetectionService.detectProviderAlarms(datasetId, providerTotals, billingPeriod);
    persistNewAlarms(providerAlarms, existingKeys, allNewAlarms);

    // Account-level alarm — computed from SQL aggregate over the full dataset-period
    double grandTotal = alarmBillingQueryService.getAccountTotal(datasetId, billingPeriod);
    alarmDetectionService
        .detectAccountAlarm(datasetId, grandTotal, billingPeriod)
        .ifPresent(alarm -> persistNewAlarms(List.of(alarm), existingKeys, allNewAlarms));

    // Resource alarms — chunked, since each record is checked independently
    int page = 0;
    int chunkSize = 1000;
    boolean hasMore = true;
    while (hasMore) {
      Page<BillingRecordEntity> chunk =
          alarmBillingQueryService.getResourceDetectionCandidates(
              datasetId, billingPeriod, PageRequest.of(page++, chunkSize));

      List<BillingRecord> chunkList = chunk.stream().map(billingMapper::mapToDomain).toList();
      List<Alarm> resourceAlarms =
          alarmDetectionService.detectResourceAlarms(datasetId, chunkList, billingPeriod);
      persistNewAlarms(resourceAlarms, existingKeys, allNewAlarms);

      hasMore = chunk.hasNext();
    }

    if (!allNewAlarms.isEmpty()) {
      evictAlarmCaches();
      notifyQuietly(allNewAlarms);
    }
  }

  /**
   * Recomputes the alarms for the daily freshly ingested incremental billing data. Should be called
   * on the latest dataset only since that is the only one we expect to have incremental changes.
   */
  public void recomputeOnIncrementalIngestion(UUID datasetId, String billingPeriod) {}

  /** Recomputes alarms across all datasets for a user. */
  @Transactional
  public void recompute(
      UUID userId, AlarmThresholdPreference oldPreference, AlarmThresholdPreference newPreference) {
    Map<UUID, String> datasets = datasetService.getBillingPeriods(userId);
    datasets.forEach(
        (datasetId, billingPeriod) ->
            recompute(datasetId, billingPeriod, oldPreference, newPreference));
  }

  /** Recomputes all the alarms in a given dataset */
  @Transactional
  public void recompute(
      UUID datasetId,
      String billingPeriod,
      AlarmThresholdPreference oldPreference,
      AlarmThresholdPreference newPreference) {
    recomputeResourceAlarms(datasetId, billingPeriod, oldPreference, newPreference);
    recomputeProviderAlarms(datasetId, billingPeriod, oldPreference, newPreference);
    recomputeAccountAlarm(datasetId, billingPeriod, oldPreference, newPreference);
    evictAlarmCaches();
  }

  private void recomputeResourceAlarms(
      UUID datasetId,
      String billingPeriod,
      AlarmThresholdPreference oldPreference,
      AlarmThresholdPreference newPreference) {
    AlarmThresholdPreference.Individual oldThresholds = oldPreference.individual();
    AlarmThresholdPreference.Individual newThresholds = newPreference.individual();

    if (oldThresholds.equals(newThresholds)) {
      return;
    }

    double lowerBound = Math.min(oldThresholds.low(), newThresholds.low());
    double upperBound = Math.max(oldThresholds.high(), newThresholds.high());

    if (lowerBound >= upperBound) {
      return;
    }

    List<BillingRecordEntity> candidates =
        alarmBillingQueryService.getResourceRecomputeCandidates(
            datasetId, billingPeriod, lowerBound, upperBound);

    for (BillingRecordEntity candidate : candidates) {
      Optional<AlarmSeverity> oldSeverity =
          resourceSeverity(candidate.getTotalCharge(), oldThresholds);
      Optional<AlarmSeverity> newSeverity =
          resourceSeverity(candidate.getTotalCharge(), newThresholds);

      if (oldSeverity.equals(newSeverity)) {
        continue;
      }

      oldSeverity.ifPresent(
          severity ->
              alarmRepository
                  .deleteByDatasetIdAndBillingPeriodAndAlarmScopeAndResourceIdAndAlarmSeverity(
                      datasetId,
                      billingPeriod,
                      AlarmScope.RESOURCE,
                      candidate.getResourceId(),
                      severity));

      newSeverity
          .map(
              severity ->
                  Alarm.resource(
                      datasetId,
                      billingPeriod,
                      severity,
                      resourceAlarmMessage(severity),
                      candidate.getResourceId(),
                      candidate.getServiceName()))
          .filter(
              alarm ->
                  !alarmRepository.existsByDatasetIdAndBillingPeriodAndBusinessKey(
                      datasetId, billingPeriod, alarm.businessKey()))
          .map(alarmMapper::mapToEntity)
          .ifPresent(alarmRepository::save);
    }
  }

  private void recomputeProviderAlarms(
      UUID datasetId,
      String billingPeriod,
      AlarmThresholdPreference oldPreference,
      AlarmThresholdPreference newPreference) {
    double oldLimit = oldPreference.provider().monthlyLimit();
    double newLimit = newPreference.provider().monthlyLimit();

    if (oldLimit == newLimit) {
      return;
    }

    Map<String, Double> providerTotals =
        alarmBillingQueryService.getProviderTotals(datasetId, billingPeriod);

    for (Map.Entry<String, Double> entry : providerTotals.entrySet()) {
      CloudProvider provider;
      try {
        provider = CloudProvider.fromString(entry.getKey());
      } catch (IllegalArgumentException ignored) {
        continue;
      }

      boolean oldShouldAlarm = entry.getValue() > oldLimit;
      boolean newShouldAlarm = entry.getValue() > newLimit;

      if (oldShouldAlarm == newShouldAlarm) {
        continue;
      }

      if (oldShouldAlarm) {
        alarmRepository.deleteByDatasetIdAndBillingPeriodAndAlarmScopeAndCloudProvider(
            datasetId, billingPeriod, AlarmScope.PROVIDER, provider);
      } else {
        Alarm alarm = Alarm.provider(datasetId, billingPeriod, provider);
        if (!alarmRepository.existsByDatasetIdAndBillingPeriodAndBusinessKey(
            datasetId, billingPeriod, alarm.businessKey())) {
          alarmRepository.save(alarmMapper.mapToEntity(alarm));
        }
      }
    }
  }

  private void recomputeAccountAlarm(
      UUID datasetId,
      String billingPeriod,
      AlarmThresholdPreference oldPreference,
      AlarmThresholdPreference newPreference) {
    double oldLowLimit = oldPreference.account().low();
    double newLowLimit = newPreference.account().low();
    double oldHighLimit = oldPreference.account().high();
    double newHighLimit = newPreference.account().high();

    if (oldLowLimit == newLowLimit && oldHighLimit == newHighLimit) {
      return;
    }

    double totalAccountCharge = alarmBillingQueryService.getAccountTotal(datasetId, billingPeriod);

    Optional<AlarmSeverity> oldSeverity =
        accountSeverity(totalAccountCharge, oldLowLimit, oldHighLimit);
    Optional<AlarmSeverity> newSeverity =
        accountSeverity(totalAccountCharge, newLowLimit, newHighLimit);

    if (oldSeverity.equals(newSeverity)) {
      return;
    }

    oldSeverity.ifPresent(
        severity ->
            alarmRepository.deleteByDatasetIdAndBillingPeriodAndAlarmScopeAndAlarmSeverity(
                datasetId, billingPeriod, AlarmScope.ACCOUNT, severity));

    newSeverity
        .map(
            severity ->
                severity == AlarmSeverity.HIGH
                    ? Alarm.accountHigh(datasetId, billingPeriod)
                    : Alarm.accountLow(datasetId, billingPeriod))
        .map(alarmMapper::mapToEntity)
        .ifPresent(alarmRepository::save);
  }

  private Optional<AlarmSeverity> accountSeverity(
      double totalAccountCharge, double lowLimit, double highLimit) {
    if (totalAccountCharge >= highLimit) {
      return Optional.of(AlarmSeverity.HIGH);
    }
    if (totalAccountCharge > lowLimit) {
      return Optional.of(AlarmSeverity.LOW);
    }
    return Optional.empty();
  }

  private Optional<AlarmSeverity> resourceSeverity(
      double totalCharge, AlarmThresholdPreference.Individual thresholds) {
    if (totalCharge >= thresholds.high()) {
      return Optional.of(AlarmSeverity.HIGH);
    }
    if (totalCharge >= thresholds.medium()) {
      return Optional.of(AlarmSeverity.MEDIUM);
    }
    if (totalCharge >= thresholds.low()) {
      return Optional.of(AlarmSeverity.LOW);
    }
    return Optional.empty();
  }

  private String resourceAlarmMessage(AlarmSeverity severity) {
    return switch (severity) {
      case LOW -> "Exceeds Charge Limit: LOW";
      case MEDIUM -> "Slightly exceeds Charge Limit: MEDIUM";
      case HIGH -> "Significantly exceeds Charge Limit (TAKE ACTION)";
    };
  }

  private void evictAlarmCaches() {
    Cache alarms = cacheManager.getCache("alarms");
    if (alarms != null) {
      alarms.clear();
    }
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
        .findByDatasetIdAndBillingPeriodAndAlarmScope(datasetId, billingPeriod, AlarmScope.PROVIDER)
        .stream()
        .map(alarmMapper::mapToDomain)
        .toList();
  }

  /** Retrieves alarms scoped to individual resources for a billing period. */
  @Cacheable(value = "alarms", key = "#datasetId + '-' + #billingPeriod + '-resource'")
  public List<Alarm> getResourceAlarmsInDataset(UUID datasetId, String billingPeriod) {
    return alarmRepository
        .findByDatasetIdAndBillingPeriodAndAlarmScope(datasetId, billingPeriod, AlarmScope.RESOURCE)
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
