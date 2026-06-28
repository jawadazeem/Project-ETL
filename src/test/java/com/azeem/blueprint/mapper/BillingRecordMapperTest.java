/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.repository.DatasetRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingRecordMapperTest {

  @Mock private DatasetRepository datasetRepository;

  private BillingRecordMapper billingRecordMapper;

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private DatasetEntity datasetEntity;

  @BeforeEach
  void setUp() {
    billingRecordMapper = new BillingRecordMapper(datasetRepository);

    datasetEntity = new DatasetEntity();
    datasetEntity.setId(DATASET_ID);
  }

  @Test
  @DisplayName("Should map BillingRecord domain object to BillingRecordEntity")
  void shouldMapToEntity() {
    when(datasetRepository.getReferenceById(DATASET_ID)).thenReturn(datasetEntity);

    BillingRecord record =
        new BillingRecord(
            DATASET_ID,
            "Acme Corporation",
            "i-0abc123def",
            "AWS",
            "2026-01",
            1240.0,
            18.75,
            320,
            249.99,
            "EC2",
            "m5.xlarge production instance");

    BillingRecordEntity result = billingRecordMapper.mapToEntity(record);

    assertThat(result).isNotNull();
    assertThat(result.getDataset()).isEqualTo(datasetEntity);
    assertThat(result.getAccountName()).isEqualTo("Acme Corporation");
    assertThat(result.getResourceId()).isEqualTo("i-0abc123def");
    assertThat(result.getCloudProvider()).isEqualTo("AWS");
    assertThat(result.getBillingPeriod()).isEqualTo("2026-01");
    assertThat(result.getComputeHours()).isEqualTo(1240.0);
    assertThat(result.getStorageGbUsed()).isEqualTo(18.75);
    assertThat(result.getApiRequests()).isEqualTo(320);
    assertThat(result.getTotalCharge()).isEqualTo(249.99);
    assertThat(result.getServiceName()).isEqualTo("EC2");
    assertThat(result.getDescription()).isEqualTo("m5.xlarge production instance");
  }

  @Test
  @DisplayName("Should map BillingRecordEntity to BillingRecord domain object")
  void shouldMapToDomain() {
    BillingRecordEntity entity = new BillingRecordEntity();
    entity.setId(1L);
    entity.setDataset(datasetEntity);
    entity.setAccountName("Acme Corporation");
    entity.setResourceId("i-0abc123def");
    entity.setCloudProvider("AWS");
    entity.setBillingPeriod("2026-01");
    entity.setComputeHours(1240.0);
    entity.setStorageGbUsed(18.75);
    entity.setApiRequests(320);
    entity.setTotalCharge(249.99);
    entity.setServiceName("EC2");
    entity.setDescription("m5.xlarge production instance");

    BillingRecord result = billingRecordMapper.mapToDomain(entity);

    assertThat(result).isNotNull();
    assertThat(result.datasetId()).isEqualTo(DATASET_ID);
    assertThat(result.accountName()).isEqualTo("Acme Corporation");
    assertThat(result.resourceId()).isEqualTo("i-0abc123def");
    assertThat(result.cloudProvider()).isEqualTo("AWS");
    assertThat(result.billingPeriod()).isEqualTo("2026-01");
    assertThat(result.computeHours()).isEqualTo(1240.0);
    assertThat(result.storageGbUsed()).isEqualTo(18.75);
    assertThat(result.apiRequests()).isEqualTo(320);
    assertThat(result.totalCharge()).isEqualTo(249.99);
    assertThat(result.serviceName()).isEqualTo("EC2");
    assertThat(result.description()).isEqualTo("m5.xlarge production instance");
  }

  @Test
  @DisplayName("Should preserve null optional fields during mapping")
  void shouldHandleNullFields() {
    when(datasetRepository.getReferenceById(DATASET_ID)).thenReturn(datasetEntity);

    BillingRecord record =
        new BillingRecord(DATASET_ID, null, null, null, "2026-01", 0.0, 0.0, 0, 0.0, null, null);

    BillingRecordEntity entity = billingRecordMapper.mapToEntity(record);

    assertThat(entity.getAccountName()).isNull();
    assertThat(entity.getResourceId()).isNull();
    assertThat(entity.getCloudProvider()).isNull();

    entity.setDataset(datasetEntity);
    BillingRecord mappedBack = billingRecordMapper.mapToDomain(entity);

    assertThat(mappedBack.datasetId()).isEqualTo(DATASET_ID);
    assertThat(mappedBack.accountName()).isNull();
    assertThat(mappedBack.resourceId()).isNull();
    assertThat(mappedBack.cloudProvider()).isNull();
  }

  @Test
  @DisplayName("Should preserve numeric values accurately during round-trip mapping")
  void shouldPreserveNumericValues() {
    when(datasetRepository.getReferenceById(DATASET_ID)).thenReturn(datasetEntity);

    BillingRecord record =
        new BillingRecord(
            DATASET_ID,
            "Enterprise Cloud",
            "arn:aws:ec2:us-east-1:123456789:instance/i-9001",
            "AWS",
            "2026-02",
            99999.0,
            9999.99,
            50000,
            123456.78,
            "EC2",
            "high-memory instance");

    BillingRecordEntity entity = billingRecordMapper.mapToEntity(record);

    assertThat(entity.getComputeHours()).isEqualTo(99999.0);
    assertThat(entity.getStorageGbUsed()).isEqualTo(9999.99);
    assertThat(entity.getApiRequests()).isEqualTo(50000);
    assertThat(entity.getTotalCharge()).isEqualTo(123456.78);

    entity.setDataset(datasetEntity);
    BillingRecord mappedBack = billingRecordMapper.mapToDomain(entity);

    assertThat(mappedBack.computeHours()).isEqualTo(99999.0);
    assertThat(mappedBack.storageGbUsed()).isEqualTo(9999.99);
    assertThat(mappedBack.apiRequests()).isEqualTo(50000);
    assertThat(mappedBack.totalCharge()).isEqualTo(123456.78);
  }
}
