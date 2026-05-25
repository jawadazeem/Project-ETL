/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.azeem.blueprint.config.SecurityConfig;
import com.azeem.blueprint.exception.core.PdfReportNotFoundException;
import com.azeem.blueprint.model.report.PdfReport;
import com.azeem.blueprint.service.report.PdfReportService;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PdfController.class)
@Import(SecurityConfig.class)
@WithMockUser
class PdfControllerTest {

  private static final String DATASET_ID = "00000000-0000-0000-0000-000000000001";
  private static final String USER_ID = "00000000-0000-0000-0000-000000000002";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PdfReportService pdfReportService;

  @Test
  @DisplayName("POST /pdf returns 200 with report metadata")
  void shouldReturn200OnGeneratePdf() throws Exception {
    UUID reportId = UUID.randomUUID();
    PdfReport report =
        new PdfReport(
            reportId,
            UUID.fromString(DATASET_ID),
            UUID.fromString(USER_ID),
            "2026-01",
            "s3/key.pdf",
            1024L,
            Instant.now());

    when(pdfReportService.generateReport(
            eq(UUID.fromString(USER_ID)), eq(UUID.fromString(DATASET_ID)), eq("2026-01")))
        .thenReturn(report);

    mockMvc
        .perform(
            post("/datasets/{datasetId}/reports/pdf", DATASET_ID)
                .param("period", "2026-01")
                .header("X-User-Id", USER_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reportId.toString()))
        .andExpect(jsonPath("$.billingPeriod").value("2026-01"));
  }

  @Test
  @DisplayName("GET /pdf/{reportId} returns PDF with correct content type")
  void shouldReturn200OnDownloadPdf() throws Exception {
    UUID reportId = UUID.randomUUID();
    byte[] pdfContent = "%PDF-1.4 test".getBytes();

    when(pdfReportService.downloadReport(UUID.fromString(DATASET_ID), reportId))
        .thenReturn(new ByteArrayInputStream(pdfContent));

    mockMvc
        .perform(get("/datasets/{datasetId}/reports/pdf/{reportId}", DATASET_ID, reportId))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"billing-report.pdf\""));
  }

  @Test
  @DisplayName("GET /pdf/{reportId} returns 404 when report not found")
  void shouldReturn404WhenReportNotFound() throws Exception {
    UUID reportId = UUID.randomUUID();

    when(pdfReportService.downloadReport(UUID.fromString(DATASET_ID), reportId))
        .thenThrow(new PdfReportNotFoundException(reportId));

    mockMvc
        .perform(get("/datasets/{datasetId}/reports/pdf/{reportId}", DATASET_ID, reportId))
        .andExpect(status().isNotFound());
  }
}
