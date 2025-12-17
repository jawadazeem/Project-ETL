package com.azeem.billing.repository;

import com.azeem.billing.entity.AlarmEntity;
import com.azeem.billing.entity.BillingRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlarmRepository extends JpaRepository<AlarmEntity, UUID> {
    List<AlarmEntity> findByBillingPeriod(String billingPeriod);
}
