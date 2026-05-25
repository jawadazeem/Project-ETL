/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.report.PdfReport;
import com.azeem.blueprint.service.report.PdfReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/datasets/{datasetId}/reports")
@Tag(name = "PDF Reports", description = "PDF report generation and download")
public class PdfController {
  private static final Logger log = LoggerFactory.getLogger(PdfController.class);

  private final PdfReportService pdfReportService;

  public PdfController(PdfReportService pdfReportService) {
    this.pdfReportService = pdfReportService;
  }

  @Operation(summary = "Generate a PDF billing report")
  @PostMapping("/pdf")
  public ResponseEntity<PdfReport> generatePdf(
      @PathVariable UUID datasetId,
      @RequestParam String period,
      @RequestHeader("X-User-Id") String userId) {
    log.info("POST /datasets/{}/reports/pdf by user={}, period={}", datasetId, userId, period);
    PdfReport report =
        pdfReportService.generateReport(UUID.fromString(userId), datasetId, period);
    return ResponseEntity.ok(report);
  }

  @Operation(summary = "Download a generated PDF report")
  @GetMapping("/pdf/{reportId}")
  public ResponseEntity<InputStreamResource> downloadPdf(
      @PathVariable UUID datasetId, @PathVariable UUID reportId) {
    log.info("GET /datasets/{}/reports/pdf/{}", datasetId, reportId);
    InputStream stream = pdfReportService.downloadReport(datasetId, reportId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"billing-report.pdf\"")
        .body(new InputStreamResource(stream));
  }
}
