/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.etl.CsvExportService;
import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.service.billing.BillingQueryService;
import com.azeem.blueprint.validation.BillingPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
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
@Tag(name = "Billing", description = "Billing record queries and analytics")
public class BillingController {
  private static final Logger log = LoggerFactory.getLogger(BillingController.class);
  private final BillingQueryService service;
  private final CsvExportService csvExportService;

  public BillingController(BillingQueryService service, CsvExportService csvExportService) {
    this.service = service;
    this.csvExportService = csvExportService;
  }

  @Operation(summary = "List all billing records with pagination")
  @GetMapping("/records")
  public ResponseEntity<Page<BillingRecord>> getRecords(
      @PathVariable UUID datasetId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    log.info("GET /datasets/{}/records called, page: {}, size: {}.", datasetId, page, size);
    return ResponseEntity.ok(service.getAllRecordsInDataset(datasetId, page, size));
  }

  @Operation(summary = "List distinct billing periods in a dataset")
  @GetMapping("/records/periods")
  public ResponseEntity<List<String>> getBillingPeriods(@PathVariable UUID datasetId) {
    log.info("GET /datasets/{}/records/periods called.", datasetId);
    return ResponseEntity.ok(service.getDistinctBillingPeriodsById(datasetId));
  }

  @Operation(summary = "List billing records for a specific period")
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

  @Operation(summary = "List billing records filtered by cloud provider")
  @GetMapping("/records/providers/{provider}")
  public ResponseEntity<Page<BillingRecord>> getRecordsByProvider(
      @PathVariable UUID datasetId,
      @PathVariable @NotBlank String provider,
      @RequestParam(required = false) String billingPeriod,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    log.info("GET /datasets/{}/records/providers/{} called.", datasetId, provider);
    if (billingPeriod != null && !billingPeriod.isBlank()) {
      return ResponseEntity.ok(
          service.getRecordsByProviderInDatasetForPeriod(
              datasetId, billingPeriod, provider, page, size));
    }

    return ResponseEntity.ok(
        service.getRecordsByProviderInDataset(datasetId, provider, page, size));
  }

  @Operation(summary = "List distinct cloud providers in a dataset")
  @GetMapping("/records/providers")
  public ResponseEntity<List<String>> getProviders(@PathVariable UUID datasetId) {
    log.info("GET /datasets/{}/records/providers called.", datasetId);
    return ResponseEntity.ok(service.getDistinctProvidersInDataset(datasetId));
  }

  @Operation(summary = "Generate billing summary for entire dataset")
  @GetMapping("/summary")
  public ResponseEntity<BillingSummary> getSummary(@PathVariable UUID datasetId) {
    log.info("GET /datasets/{}/summary called.", datasetId);
    return ResponseEntity.ok(service.generateSummary(datasetId));
  }

  @Operation(summary = "Generate billing summary for a specific period")
  @GetMapping("/summary/periods/{billingPeriod}")
  public ResponseEntity<BillingSummary> getSummaryByPeriod(
      @PathVariable UUID datasetId, @BillingPeriod @PathVariable String billingPeriod) {
    log.info("GET /datasets/{}/summary/periods/{} called.", datasetId, billingPeriod);
    return ResponseEntity.ok(service.generateSummaryForPeriodInDataset(datasetId, billingPeriod));
  }

  @Operation(summary = "Get top N records by total charge")
  @GetMapping("/top/{n}")
  public ResponseEntity<Page<BillingRecord>> getTopN(
      @PathVariable UUID datasetId, @PathVariable @Min(1) @Max(100) int n) {
    log.info("GET /datasets/{}/top/{} called.", datasetId, n);
    return ResponseEntity.ok(service.getTopNRecordsInDataset(datasetId, n));
  }

  @Operation(summary = "Export billing records as CSV")
  @GetMapping(value = "/records/export", produces = "text/csv")
  public void exportRecords(
      @PathVariable UUID datasetId,
      @RequestParam @BillingPeriod String billingPeriod,
      HttpServletResponse response)
      throws IOException {
    log.info("GET /datasets/{}/records/export called for period {}.", datasetId, billingPeriod);
    response.setContentType("text/csv");
    response.setHeader(
        "Content-Disposition", "attachment; filename=\"billing-" + billingPeriod + ".csv\"");
    List<BillingRecord> records = service.getAllRecordsForExport(datasetId, billingPeriod);
    csvExportService.writeRecords(records, response.getOutputStream());
  }
}
