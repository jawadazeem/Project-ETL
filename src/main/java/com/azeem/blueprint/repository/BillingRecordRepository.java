/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import com.azeem.blueprint.entity.BillingRecordEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BillingRecordRepository extends JpaRepository<BillingRecordEntity, Long> {
  @Query(
      """
    SELECT DISTINCT b.billingPeriod
    FROM BillingRecordEntity b
    WHERE b.dataset.id = :datasetId
    ORDER BY b.billingPeriod
  """)
  Page<String> findBillingPeriodByDatasetId(@Param("datasetId") UUID datasetId, Pageable pageable);

  @Query(
      """
      SELECT DISTINCT b.cloudProvider
      FROM BillingRecordEntity b
      WHERE b.dataset.id = :datasetId
      ORDER BY b.cloudProvider
    """)
  List<String> findDistinctCloudProvidersByDatasetId(@Param("datasetId") UUID datasetId);

  @Query(
      """
      SELECT DISTINCT b.billingPeriod
      FROM BillingRecordEntity b
      WHERE b.dataset.id = :datasetId
      ORDER BY b.billingPeriod
    """)
  List<String> findDistinctBillingPeriodByDatasetId(@Param("datasetId") UUID datasetId);

  @Query(
      """
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM BillingRecordEntity b
        WHERE b.dataset.id = :datasetId AND b.billingPeriod = :period
      """)
  boolean existsByDatasetIdAndBillingPeriod(
      @Param("datasetId") UUID datasetId, @Param("period") String period);

  @NotNull
  Page<BillingRecordEntity> findByDatasetId(UUID datasetId, Pageable pageable);

  Page<BillingRecordEntity> findByDatasetIdAndBillingPeriod(
      UUID datasetId, String billingPeriod, Pageable pageable);

  List<BillingRecordEntity> findByDatasetIdAndBillingPeriod(UUID datasetId, String billingPeriod);

  Page<BillingRecordEntity> findByDatasetIdAndCloudProviderIgnoreCase(
      UUID datasetId, String cloudProvider, Pageable pageable);

  Page<BillingRecordEntity> findByDatasetIdAndBillingPeriodAndCloudProviderIgnoreCase(
      UUID datasetId, String billingPeriod, String cloudProvider, Pageable pageable);

  long countByDatasetId(UUID datasetId);

  long countByDatasetIdIn(Collection<UUID> datasetIds);

  @Query(
      """
      SELECT b.cloudProvider, SUM(b.totalCharge)
      FROM BillingRecordEntity b
      WHERE b.dataset.id = :datasetId AND b.billingPeriod = :billingPeriod
        AND b.cloudProvider IS NOT NULL
      GROUP BY b.cloudProvider
    """)
  List<Object[]> sumTotalChargeGroupedByCloudProvider(
      @Param("datasetId") UUID datasetId, @Param("billingPeriod") String billingPeriod);

  @Query(
      """
      SELECT COALESCE(SUM(b.totalCharge), 0)
      FROM BillingRecordEntity b
      WHERE b.dataset.id = :datasetId AND b.billingPeriod = :billingPeriod
    """)
  double sumTotalChargeByDatasetIdAndBillingPeriod(
      @Param("datasetId") UUID datasetId, @Param("billingPeriod") String billingPeriod);

  @Modifying
  @Transactional
  int deleteByDatasetIdAndBillingPeriod(UUID datasetId, String billingPeriod);
}
