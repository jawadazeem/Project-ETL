package com.azeem.billing.repository;

import com.azeem.billing.entity.BillingRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


@Repository
public interface BillingRecordRepository extends JpaRepository<BillingRecordEntity, Long> {

    @Query("SELECT DISTINCT b.billingPeriod FROM BillingRecordEntity b ORDER BY b.billingPeriod")
    List<String> findAllBillingPeriods();
    // Purpose: To find billing records by billing period.
    List<BillingRecordEntity> findByBillingPeriod(String billingPeriod);
}

