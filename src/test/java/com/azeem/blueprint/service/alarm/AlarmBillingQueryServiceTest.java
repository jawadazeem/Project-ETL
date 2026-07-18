/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.repository.BillingRecordRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AlarmBillingQueryServiceTest {

  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String BILLING_PERIOD = "2026-01";

  @Mock private BillingRecordRepository billingRecordRepository;

  @InjectMocks private AlarmBillingQueryService service;

  @Test
  void shouldMapProviderTotalsByProviderName() {
    when(billingRecordRepository.sumTotalChargeGroupedByCloudProvider(DATASET_ID, BILLING_PERIOD))
        .thenReturn(
            List.of(
                new Object[] {"AWS", 1500.0},
                new Object[] {"AZURE", 2500.0},
                new Object[] {null, 3000.0},
                new Object[] {"GCP", null}));

    Map<String, Double> result = service.getProviderTotals(DATASET_ID, BILLING_PERIOD);

    assertThat(result).containsEntry("AWS", 1500.0).containsEntry("AZURE", 2500.0).hasSize(2);
  }

  @Test
  void shouldDelegateAccountTotalQuery() {
    when(billingRecordRepository.sumTotalChargeByDatasetIdAndBillingPeriod(
            DATASET_ID, BILLING_PERIOD))
        .thenReturn(4500.0);

    assertThat(service.getAccountTotal(DATASET_ID, BILLING_PERIOD)).isEqualTo(4500.0);
  }

  @Test
  void shouldDelegateResourceDetectionCandidateQuery() {
    PageRequest pageable = PageRequest.of(0, 1000);
    Page<BillingRecordEntity> page = new PageImpl<>(List.of(new BillingRecordEntity()));
    when(billingRecordRepository.findByDatasetIdAndBillingPeriod(
            DATASET_ID, BILLING_PERIOD, pageable))
        .thenReturn(page);

    assertThat(service.getResourceDetectionCandidates(DATASET_ID, BILLING_PERIOD, pageable))
        .isSameAs(page);
  }

  @Test
  void shouldDelegateResourceRecomputeCandidateQuery() {
    List<BillingRecordEntity> candidates = List.of(new BillingRecordEntity());
    when(billingRecordRepository.findResourceAlarmRecomputeCandidates(
            DATASET_ID, BILLING_PERIOD, 1000.0, 6000.0))
        .thenReturn(candidates);

    assertThat(service.getResourceRecomputeCandidates(DATASET_ID, BILLING_PERIOD, 1000.0, 6000.0))
        .isSameAs(candidates);
    verify(billingRecordRepository)
        .findResourceAlarmRecomputeCandidates(DATASET_ID, BILLING_PERIOD, 1000.0, 6000.0);
  }
}
