/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.service.billing.BillingQueryService;
import com.azeem.blueprint.validation.BillingPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/datasets/{datasetId}")
public class BillingController {
  private static final Logger log = LoggerFactory.getLogger(BillingController.class);
  private final BillingQueryService service;

  public BillingController(BillingQueryService service) {
    this.service = service;
  }

  @GetMapping("/records")
  public ResponseEntity<Page<BillingRecord>> getRecords(
      @PathVariable UUID datasetId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    log.info("GET /datasets/{}/records called, page: {}, size: {}.", datasetId, page, size);
    return ResponseEntity.ok(service.getAllRecordsInDataset(datasetId, page, size));
  }

  @GetMapping("/records/periods")
  public ResponseEntity<List<String>> getBillingPeriods(@PathVariable UUID datasetId) {
    log.info("GET /datasets/{}/records/periods called.", datasetId);
    return ResponseEntity.ok(service.getDistinctBillingPeriodsById(datasetId));
  }

  @GetMapping("/records/periods/{billingPeriod}")
  public ResponseEntity<Page<BillingRecord>> getRecordsByPeriod(
      @PathVariable UUID datasetId,
      @BillingPeriod @PathVariable String billingPeriod,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    log.info(
        "GET /datasets/{}/records/periods/{} called, page: {}, size: {}.",
        datasetId,
        billingPeriod,
        page,
        size);
    return ResponseEntity.ok(
        service.getDatasetRecordsByPeriod(datasetId, billingPeriod, page, size));
  }

  @GetMapping("/records/departments/{department}")
  public ResponseEntity<Page<BillingRecord>> getRecordsByDepartment(
      @PathVariable UUID datasetId,
      @PathVariable @NotBlank String department,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    log.info("GET /datasets/{}/records/departments/{} called.", datasetId, department);
    return ResponseEntity.ok(
        service.getRecordsByDepartmentInDataset(datasetId, department, page, size));
  }

  @GetMapping("/summary")
  public ResponseEntity<BillingSummary> getSummary(@PathVariable UUID datasetId) {
    log.info("GET /datasets/{}/summary called.", datasetId);
    return ResponseEntity.ok(service.generateSummary(datasetId));
  }

  @GetMapping("/summary/periods/{billingPeriod}")
  public ResponseEntity<BillingSummary> getSummaryByPeriod(
      @PathVariable UUID datasetId, @BillingPeriod @PathVariable String billingPeriod) {
    log.info("GET /datasets/{}/summary/periods/{} called.", datasetId, billingPeriod);
    return ResponseEntity.ok(service.generateSummaryForPeriodInDataset(datasetId, billingPeriod));
  }

  @GetMapping("/top/{n}")
  public ResponseEntity<Page<BillingRecord>> getTopN(
      @PathVariable UUID datasetId, @PathVariable @Min(1) @Max(100) int n) {
    log.info("GET /datasets/{}/top/{} called.", datasetId, n);
    return ResponseEntity.ok(service.getTopNRecordsInDataset(datasetId, n));
  }
}
