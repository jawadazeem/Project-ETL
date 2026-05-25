/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.model.billing.BillingRecord;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.service.billing.BillingQueryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingController.class)
@WithMockUser
class BillingControllerTest {
  private static final String DATASET_ID = "00000000-0000-0000-0000-000000000001";
  private static final String BASE = "/datasets/" + DATASET_ID;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BillingQueryService billingQueryService;
  @MockitoBean private com.azeem.blueprint.etl.CsvExportService csvExportService;

  @Test
  void defaultPageAndSize_shouldReturnAllRecords() throws Exception {
    mockMvc.perform(get(BASE + "/records")).andExpect(status().isOk());

    verify(billingQueryService).getAllRecordsInDataset(any(UUID.class), eq(0), eq(20));
  }

  @Test
  void customPageAndSize_shouldReturnAllRecords() throws Exception {
    mockMvc
        .perform(get(BASE + "/records").param("page", "1").param("size", "5"))
        .andExpect(status().isOk());

    verify(billingQueryService).getAllRecordsInDataset(any(UUID.class), eq(1), eq(5));
  }

  @Test
  void shouldReturnBillingPeriods() throws Exception {
    mockMvc.perform(get(BASE + "/records/periods")).andExpect(status().isOk());

    verify(billingQueryService).getDistinctBillingPeriodsById(any(UUID.class));
  }

  @Test
  void shouldReturnDepartments() throws Exception {
    when(billingQueryService.getDistinctDepartmentsInDataset(any(UUID.class)))
        .thenReturn(List.of("Finance", "IT"));

    mockMvc.perform(get(BASE + "/records/departments")).andExpect(status().isOk());

    verify(billingQueryService).getDistinctDepartmentsInDataset(any(UUID.class));
  }

  @Test
  void defaultPageAndSize_shouldReturnRecordsByPeriod() throws Exception {
    Page<BillingRecord> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    when(billingQueryService.getDatasetRecordsByPeriod(any(), anyString(), anyInt(), anyInt()))
        .thenReturn(page);

    mockMvc.perform(get(BASE + "/records/periods/2026-01")).andExpect(status().isOk());

    verify(billingQueryService)
        .getDatasetRecordsByPeriod(any(UUID.class), eq("2026-01"), eq(0), eq(20));
  }

  @Test
  void customPageAndSize_shouldReturnRecordsByPeriod() throws Exception {
    when(billingQueryService.getDatasetRecordsByPeriod(any(), anyString(), anyInt(), anyInt()))
        .thenReturn(Page.empty());

    mockMvc
        .perform(get(BASE + "/records/periods/2026-01").param("page", "1").param("size", "10"))
        .andExpect(status().isOk());

    verify(billingQueryService)
        .getDatasetRecordsByPeriod(any(UUID.class), eq("2026-01"), eq(1), eq(10));
  }

  @Test
  void shouldRejectInvalidBillingPeriodFormat() throws Exception {
    mockMvc.perform(get(BASE + "/records/periods/70-2070")).andExpect(status().isBadRequest());

    verify(billingQueryService, never())
        .getDatasetRecordsByPeriod(any(), anyString(), anyInt(), anyInt());
  }

  @Test
  void defaultPageAndSize_shouldReturnRecordsByDepartment() throws Exception {
    mockMvc.perform(get(BASE + "/records/departments/IT")).andExpect(status().isOk());

    verify(billingQueryService)
        .getRecordsByDepartmentInDataset(any(UUID.class), eq("IT"), eq(0), eq(20));
  }

  @Test
  void customPageAndSize_shouldReturnRecordsByDepartment() throws Exception {
    mockMvc
        .perform(get(BASE + "/records/departments/IT").param("page", "1").param("size", "20"))
        .andExpect(status().isOk());

    verify(billingQueryService)
        .getRecordsByDepartmentInDataset(any(UUID.class), eq("IT"), eq(1), eq(20));
  }

  @Test
  void shouldReturnRecordsByDepartmentAndBillingPeriod() throws Exception {
    mockMvc
        .perform(
            get(BASE + "/records/departments/IT")
                .param("billingPeriod", "2026-01")
                .param("page", "1")
                .param("size", "20"))
        .andExpect(status().isOk());

    verify(billingQueryService)
        .getRecordsByDepartmentInDatasetForPeriod(
            any(UUID.class), eq("2026-01"), eq("IT"), eq(1), eq(20));
    verify(billingQueryService, never())
        .getRecordsByDepartmentInDataset(any(UUID.class), anyString(), anyInt(), anyInt());
  }

  @Test
  void shouldReturnBillingSummary() throws Exception {
    mockMvc.perform(get(BASE + "/summary")).andExpect(status().isOk());

    verify(billingQueryService).generateSummary(any(UUID.class));
  }

  @Test
  void shouldReturnBillingSummaryByPeriod() throws Exception {
    BillingSummary summary = new BillingSummary();
    when(billingQueryService.generateSummaryForPeriodInDataset(any(), eq("2026-01")))
        .thenReturn(summary);

    mockMvc.perform(get(BASE + "/summary/periods/2026-01")).andExpect(status().isOk());

    verify(billingQueryService).generateSummaryForPeriodInDataset(any(UUID.class), eq("2026-01"));
  }

  @Test
  void shouldReturnTopNRecords() throws Exception {
    mockMvc.perform(get(BASE + "/top/5")).andExpect(status().isOk());

    verify(billingQueryService).getTopNRecordsInDataset(any(UUID.class), eq(5));
  }

  @Test
  void shouldRejectTopNAboveMaximum() throws Exception {
    mockMvc.perform(get(BASE + "/top/101")).andExpect(status().isBadRequest());

    verify(billingQueryService, never()).getTopNRecordsInDataset(any(), anyInt());
  }
}
