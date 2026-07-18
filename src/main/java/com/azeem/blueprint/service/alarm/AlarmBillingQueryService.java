/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.alarm;

import com.azeem.blueprint.entity.BillingRecordEntity;
import com.azeem.blueprint.repository.BillingRecordRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlarmBillingQueryService {
  private final BillingRecordRepository billingRecordRepository;

  public AlarmBillingQueryService(BillingRecordRepository billingRecordRepository) {
    this.billingRecordRepository = billingRecordRepository;
  }

  public Map<String, Double> getProviderTotals(UUID datasetId, String billingPeriod) {
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

  public double getAccountTotal(UUID datasetId, String billingPeriod) {
    return billingRecordRepository.sumTotalChargeByDatasetIdAndBillingPeriod(
        datasetId, billingPeriod);
  }

  public Page<BillingRecordEntity> getResourceDetectionCandidates(
      UUID datasetId, String billingPeriod, Pageable pageable) {
    return billingRecordRepository.findByDatasetIdAndBillingPeriod(
        datasetId, billingPeriod, pageable);
  }

  public List<BillingRecordEntity> getResourceRecomputeCandidates(
      UUID datasetId, String billingPeriod, double lowerBound, double upperBound) {
    return billingRecordRepository.findResourceAlarmRecomputeCandidates(
        datasetId, billingPeriod, lowerBound, upperBound);
  }
}
