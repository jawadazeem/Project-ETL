/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.AlarmEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.repository.dataset.DatasetRepository;
import org.springframework.stereotype.Component;

/** Mapper class to convert between Alarm domain model and AlarmEntity database entity. */
@Component
public class AlarmMapper {
  private final DatasetRepository datasetRepository;

  public AlarmMapper(DatasetRepository datasetRepository) {
    this.datasetRepository = datasetRepository;
  }

  public AlarmEntity mapToEntity(Alarm alarm) {
    AlarmEntity alarmEntity = new AlarmEntity();
    alarmEntity.setBusinessKey(alarm.businessKey());
    alarmEntity.setDataset(getDatasetById(alarm));
    alarmEntity.setAlarmScope(alarm.alarmScope());
    alarmEntity.setAlarmSeverity(alarm.alarmSeverity());
    alarmEntity.setAlarmType(alarm.alarmType());
    alarmEntity.setBillingPeriod(alarm.billingPeriod());
    alarmEntity.setResourceId(alarm.resourceId());
    alarmEntity.setServiceName(alarm.serviceName());
    alarmEntity.setCloudProvider(alarm.cloudProvider());
    alarmEntity.setExplanation(alarm.explanation());
    alarmEntity.setTimestamp(alarm.timestamp());
    return alarmEntity;
  }

  public Alarm mapToDomain(AlarmEntity alarmEntity) {
    return new Alarm(
        alarmEntity.getId(),
        alarmEntity.getDataset().getId(),
        alarmEntity.getBusinessKey(),
        alarmEntity.getAlarmScope(),
        alarmEntity.getBillingPeriod(),
        alarmEntity.getAlarmType(),
        alarmEntity.getAlarmSeverity(),
        alarmEntity.getExplanation(),
        alarmEntity.getTimestamp(),
        alarmEntity.getResourceId(),
        alarmEntity.getServiceName(),
        alarmEntity.getCloudProvider());
  }

  private DatasetEntity getDatasetById(Alarm alarm) {
    return datasetRepository.getReferenceById(alarm.datasetId());
  }
}
