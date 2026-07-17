/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.exception.infra.DatasetNotFoundException;
import com.azeem.blueprint.mapper.DatasetMapper;
import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.repository.dataset.DatasetRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.billing.BillingS3Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

  private static final UUID DATASET_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private DatasetRepository datasetRepository;
  @Mock private BillingRecordRepository billingRecordRepository;
  @Mock private BillingS3Service s3Service;
  @Mock private DatasetMapper datasetMapper;
  @Mock private AppUserService appUserService;

  @InjectMocks private DatasetService datasetService;

  private Dataset sampleDataset() {
    return new Dataset(
        DATASET_ID,
        OWNER_ID,
        "2026-01",
        "billing.csv",
        "key/billing.csv",
        Instant.parse("2026-01-01T00:00:00Z"),
        "READY",
        false);
  }

  private DatasetEntity sampleEntity() {
    DatasetEntity entity = new DatasetEntity();
    entity.setId(DATASET_ID);
    entity.setSourceFilename("billing.csv");
    entity.setStatus("READY");
    return entity;
  }

  @Test
  @DisplayName("listDatasets returns mapped datasets for a given owner")
  void listDatasets_returnsDatasets() {
    DatasetEntity entity = sampleEntity();
    when(datasetRepository.findActiveDatasets(OWNER_ID)).thenReturn(List.of(entity));
    when(datasetMapper.mapToDomain(entity)).thenReturn(sampleDataset());

    List<Dataset> result = datasetService.listDatasets(OWNER_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(DATASET_ID);
    verify(datasetRepository).findActiveDatasets(OWNER_ID);
    verify(datasetMapper).mapToDomain(entity);
  }

  @Test
  @DisplayName("listDatasets returns empty list when owner has no datasets")
  void listDatasets_noDatasets_returnsEmpty() {
    when(datasetRepository.findActiveDatasets(OWNER_ID)).thenReturn(List.of());

    List<Dataset> result = datasetService.listDatasets(OWNER_ID);

    assertThat(result).isEmpty();
    verify(datasetMapper, never()).mapToDomain(any());
  }

  @Test
  @DisplayName("getDataset returns dataset when it exists")
  void getDataset_existingId_returnsDataset() {
    DatasetEntity entity = sampleEntity();
    when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.of(entity));
    when(datasetMapper.mapToDomain(entity)).thenReturn(sampleDataset());

    Dataset result = datasetService.getDataset(DATASET_ID);

    assertThat(result.id()).isEqualTo(DATASET_ID);
    verify(datasetRepository).findById(DATASET_ID);
  }

  @Test
  @DisplayName("getDataset throws DatasetNotFoundException when dataset does not exist")
  void getDataset_unknownId_throwsDatasetNotFoundException() {
    when(datasetRepository.findById(DATASET_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> datasetService.getDataset(DATASET_ID))
        .isInstanceOf(DatasetNotFoundException.class);
  }

  @Test
  @DisplayName("deleteRecordsByPeriodInDataset succeeds when dataset exists")
  void deleteRecordsByPeriodInDataset_existingDataset_deletesRecords() {
    when(datasetRepository.existsById(DATASET_ID)).thenReturn(true);
    when(billingRecordRepository.deleteByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01"))
        .thenReturn(5);

    datasetService.deleteRecordsByPeriodInDataset(DATASET_ID, "2026-01");

    verify(billingRecordRepository).deleteByDatasetIdAndBillingPeriod(DATASET_ID, "2026-01");
  }

  @Test
  @DisplayName("deleteRecordsByPeriodInDataset throws when dataset does not exist")
  void deleteRecordsByPeriodInDataset_unknownDataset_throwsDatasetNotFoundException() {
    when(datasetRepository.existsById(DATASET_ID)).thenReturn(false);

    assertThatThrownBy(() -> datasetService.deleteRecordsByPeriodInDataset(DATASET_ID, "2026-01"))
        .isInstanceOf(DatasetNotFoundException.class);

    verify(billingRecordRepository, never()).deleteByDatasetIdAndBillingPeriod(any(), any());
  }
}
