/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import com.azeem.blueprint.entity.BillingRecordEntity;
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

  // DB-level distinct departments
  @Query(
      """
      SELECT DISTINCT b.department
      FROM BillingRecordEntity b
      WHERE b.dataset.id = :datasetId
      ORDER BY b.department
    """)
  List<String> findDistinctDepartmentsByDatasetId(@Param("datasetId") UUID datasetId);

  // DB-level distinct billing periods (non-paged)
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

  Page<BillingRecordEntity> findByDatasetIdAndDepartmentIgnoreCase(
      UUID datasetId, String department, Pageable pageable);

  long countByDatasetId(UUID datasetId);

  @Query(
      """
      SELECT b.department, SUM(b.totalCharge)
      FROM BillingRecordEntity b
      WHERE b.dataset.id = :datasetId AND b.billingPeriod = :billingPeriod
        AND b.department IS NOT NULL
      GROUP BY b.department
    """)
  List<Object[]> sumTotalChargeGroupedByDepartment(
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
