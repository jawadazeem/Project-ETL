/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.dataset.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.repository.BillingRecordRepository;
import com.azeem.blueprint.repository.DatasetRepository;
import com.azeem.blueprint.service.billing.BillingIngestionService;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoDatasetLoaderTest {

  private static final UUID DEMO_DATASET_ID = new UUID(0L, 0L);

  @Mock private BillingIngestionService billingIngestionService;
  @Mock private BillingRecordRepository billingRecordRepository;
  @Mock private DatasetRepository datasetRepository;

  @InjectMocks private DemoDatasetLoader loader;

  @Test
  void shouldLoadDemoDataWhenNotAlreadyLoaded() throws Exception {
    when(billingRecordRepository.existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data"))
        .thenReturn(false);
    DatasetEntity dataset = demoDataset();
    when(datasetRepository.findById(DEMO_DATASET_ID))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(dataset));
    when(datasetRepository.save(any(DatasetEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    loader.loadDemoData();

    verify(billingRecordRepository)
        .existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data");
    verify(billingIngestionService, atLeastOnce())
        .ingestData(eq(DEMO_DATASET_ID), any(InputStream.class));
    verify(datasetRepository, atLeastOnce()).save(any(DatasetEntity.class));
  }

  @Test
  void shouldNotLoadDemoDataWhenAlreadyLoaded() {
    when(billingRecordRepository.existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data"))
        .thenReturn(true);
    when(datasetRepository.findById(DEMO_DATASET_ID)).thenReturn(Optional.of(demoDataset()));

    loader.loadDemoData();

    verify(billingRecordRepository)
        .existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data");
    verifyNoInteractions(billingIngestionService);
    verify(datasetRepository).save(any(DatasetEntity.class));
  }

  @Test
  void shouldHandleRuntimeExceptionFromIngestionGracefully() {
    when(billingRecordRepository.existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data"))
        .thenReturn(false);
    DatasetEntity dataset = demoDataset();
    when(datasetRepository.findById(DEMO_DATASET_ID))
        .thenReturn(Optional.of(dataset))
        .thenReturn(Optional.of(dataset));

    doThrow(new RuntimeException("ingestion failure"))
        .when(billingIngestionService)
        .ingestData(any(UUID.class), any(InputStream.class));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> loader.loadDemoData())
        .isInstanceOf(RuntimeException.class);

    verify(datasetRepository, atLeastOnce()).save(dataset);
  }

  @Test
  void shouldAlwaysCheckIfDataAlreadyLoaded() {
    when(billingRecordRepository.existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data"))
        .thenReturn(false);
    DatasetEntity dataset = demoDataset();
    when(datasetRepository.findById(DEMO_DATASET_ID))
        .thenReturn(Optional.of(dataset))
        .thenReturn(Optional.of(dataset));

    loader.loadDemoData();

    verify(billingRecordRepository)
        .existsByDatasetIdAndBillingPeriod(DEMO_DATASET_ID, "dummy-data");
  }

  private DatasetEntity demoDataset() {
    DatasetEntity dataset = new DatasetEntity();
    dataset.setId(DEMO_DATASET_ID);
    dataset.setBillingPeriod("dummy-data");
    dataset.setSourceFilename("dummy-data.csv");
    dataset.setStatus("LOADING");
    return dataset;
  }
}
