/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.etl;

import com.azeem.blueprint.model.billing.BillingRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Transforms raw tabular billing data into {@link BillingRecord} domain objects.
 *
 * <p>This class represents the <b>Transform</b> phase of the ingestion pipeline.
 *
 * <p>The assembler interprets column positions, performs type conversion, and constructs validated
 * domain records.
 *
 * <p>This component is stateless and thread-safe by design.
 *
 * <h3>Responsibilities</h3>
 *
 * <ul>
 *   <li>Interpret column ordering
 *   <li>Convert string values to domain types
 *   <li>Create {@link BillingRecord} instances
 * </ul>
 *
 * <h3>Failure Behavior</h3>
 *
 * <p>Malformed rows or conversion errors result in runtime exceptions, terminating ingestion.
 */
@Component
public class BillingRecordAssembler {
  Logger log = LoggerFactory.getLogger(BillingRecordAssembler.class);

  public BillingRecordAssembler() {}

  public List<BillingRecord> assembleRecords(List<String[]> entries, UUID datasetId) {
    List<BillingRecord> records = new ArrayList<>();
    for (String[] entry : entries) {
      records.add(assembleRecord(entry, datasetId));
    }
    log.info("Assembled {} BillingRecord instances from raw data.", records.size());
    return records;
  }

  public BillingRecord assembleRecord(String[] entry, UUID datasetId) {
    String accountName = entry[0];
    String resourceId = entry[1];
    String cloudProvider = entry[2];
    String billingPeriod = entry[3];

    double computeHours = Double.parseDouble(entry[4]);
    double storageGbUsed = Double.parseDouble(entry[5]);
    long apiRequests = Long.parseLong(entry[6]);
    double totalCharge = Double.parseDouble(entry[7]);

    String serviceName = entry[8];
    String description = entry.length > 9 ? entry[9] : "";

    return new BillingRecord(
        datasetId,
        accountName,
        resourceId,
        cloudProvider,
        billingPeriod,
        computeHours,
        storageGbUsed,
        apiRequests,
        totalCharge,
        serviceName,
        description);
  }
}
