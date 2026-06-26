/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.azeem.blueprint.entity.AlarmEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.alarm.AlarmScope;
import com.azeem.blueprint.model.alarm.AlarmSeverity;
import com.azeem.blueprint.model.billing.CloudProvider;
import com.azeem.blueprint.repository.DatasetRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmMapperTest {

  @Mock private DatasetRepository datasetRepository;

  private AlarmMapper alarmMapper;

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID BUSINESS_KEY = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final Instant TIMESTAMP = Instant.parse("2026-01-15T10:15:30Z");

  private DatasetEntity datasetEntity;

  @BeforeEach
  void setUp() {
    alarmMapper = new AlarmMapper(datasetRepository);

    datasetEntity = new DatasetEntity();
    datasetEntity.setId(DATASET_ID);
  }

  @Test
  @DisplayName("Should map Alarm domain object to AlarmEntity")
  void shouldMapToEntity() {
    when(datasetRepository.getReferenceById(DATASET_ID)).thenReturn(datasetEntity);

    Alarm alarm =
        new Alarm(
            ID,
            DATASET_ID,
            BUSINESS_KEY,
            AlarmScope.ACCOUNT,
            "2026-01",
            "OVERAGE",
            AlarmSeverity.HIGH,
            "Usage exceeded threshold",
            TIMESTAMP,
            "i-0abc123",
            "EC2",
            CloudProvider.AWS);

    AlarmEntity result = alarmMapper.mapToEntity(alarm);

    assertThat(result).isNotNull();
    assertThat(result.getDataset()).isEqualTo(datasetEntity);
    assertThat(result.getBusinessKey()).isEqualTo(BUSINESS_KEY);
    assertThat(result.getAlarmScope()).isEqualTo(AlarmScope.ACCOUNT);
    assertThat(result.getBillingPeriod()).isEqualTo("2026-01");
    assertThat(result.getAlarmType()).isEqualTo("OVERAGE");
    assertThat(result.getAlarmSeverity()).isEqualTo(AlarmSeverity.HIGH);
    assertThat(result.getExplanation()).isEqualTo("Usage exceeded threshold");
    assertThat(result.getTimestamp()).isEqualTo(TIMESTAMP);
    assertThat(result.getResourceId()).isEqualTo("i-0abc123");
    assertThat(result.getServiceName()).isEqualTo("EC2");
    assertThat(result.getCloudProvider()).isEqualTo(CloudProvider.AWS);
  }

  @Test
  @DisplayName("Should map AlarmEntity to Alarm domain object")
  void shouldMapToDomain() {
    AlarmEntity entity = new AlarmEntity();
    entity.setId(ID);
    entity.setDataset(datasetEntity);
    entity.setBusinessKey(BUSINESS_KEY);
    entity.setAlarmScope(AlarmScope.ACCOUNT);
    entity.setBillingPeriod("2026-01");
    entity.setAlarmType("OVERAGE");
    entity.setAlarmSeverity(AlarmSeverity.HIGH);
    entity.setExplanation("Usage exceeded threshold");
    entity.setTimestamp(TIMESTAMP);
    entity.setResourceId("i-0abc123");
    entity.setServiceName("EC2");
    entity.setCloudProvider(CloudProvider.AWS);

    Alarm result = alarmMapper.mapToDomain(entity);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(ID);
    assertThat(result.datasetId()).isEqualTo(DATASET_ID);
    assertThat(result.businessKey()).isEqualTo(BUSINESS_KEY);
    assertThat(result.alarmScope()).isEqualTo(AlarmScope.ACCOUNT);
    assertThat(result.billingPeriod()).isEqualTo("2026-01");
    assertThat(result.alarmType()).isEqualTo("OVERAGE");
    assertThat(result.alarmSeverity()).isEqualTo(AlarmSeverity.HIGH);
    assertThat(result.explanation()).isEqualTo("Usage exceeded threshold");
    assertThat(result.timestamp()).isEqualTo(TIMESTAMP);
    assertThat(result.resourceId()).isEqualTo("i-0abc123");
    assertThat(result.serviceName()).isEqualTo("EC2");
    assertThat(result.cloudProvider()).isEqualTo(CloudProvider.AWS);
  }

  @Test
  @DisplayName("Should preserve null optional fields during mapping")
  void shouldHandleNullOptionalFields() {
    when(datasetRepository.getReferenceById(DATASET_ID)).thenReturn(datasetEntity);

    Alarm alarm =
        new Alarm(
            ID,
            DATASET_ID,
            BUSINESS_KEY,
            AlarmScope.ACCOUNT,
            "2026-01",
            "OVERAGE",
            AlarmSeverity.MEDIUM,
            "Provider not specified",
            TIMESTAMP,
            null,
            null,
            null);

    AlarmEntity entity = alarmMapper.mapToEntity(alarm);

    assertThat(entity.getResourceId()).isNull();
    assertThat(entity.getServiceName()).isNull();
    assertThat(entity.getCloudProvider()).isNull();

    entity.setDataset(datasetEntity);
    Alarm mappedBack = alarmMapper.mapToDomain(entity);

    assertThat(mappedBack.resourceId()).isNull();
    assertThat(mappedBack.serviceName()).isNull();
    assertThat(mappedBack.cloudProvider()).isNull();
  }
}
