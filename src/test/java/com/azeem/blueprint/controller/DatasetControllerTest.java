/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.azeem.blueprint.config.SecurityConfig;
import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.service.dataset.DatasetService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DatasetController.class)
@Import(SecurityConfig.class)
class DatasetControllerTest {

  private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
  private static final UUID USER_UUID = UUID.fromString(USER_ID);
  private static final UUID DATASET_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DatasetService datasetService;

  private Dataset sampleDataset() {
    return new Dataset(
        DATASET_ID,
        USER_UUID,
        "2026-01",
        "billing.csv",
        "key/billing.csv",
        Instant.parse("2026-01-01T00:00:00Z"),
        "PENDING_INGESTION");
  }

  @Test
  @DisplayName("POST /datasets returns 200 with a valid CSV file and X-User-Id header")
  void postDataset_validRequest_returns200() throws Exception {
    when(datasetService.initializeAndUploadDataset(anyString(), any())).thenReturn(sampleDataset());

    MockMultipartFile file =
        new MockMultipartFile("file", "billing.csv", MediaType.TEXT_PLAIN_VALUE, "data".getBytes());

    mockMvc
        .perform(multipart("/datasets").file(file).header("X-User-Id", USER_ID))
        .andExpect(status().isOk());

    verify(datasetService).initializeAndUploadDataset(eq(USER_ID), any());
  }

  @Test
  @DisplayName("POST /datasets returns 400 when file is not a CSV")
  void postDataset_nonCsvFile_returns400() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "report.xlsx", MediaType.TEXT_PLAIN_VALUE, "data".getBytes());

    mockMvc
        .perform(multipart("/datasets").file(file).header("X-User-Id", USER_ID))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /datasets returns 200 with list of datasets")
  void getDatasets_validRequest_returns200() throws Exception {
    when(datasetService.listDatasets(USER_UUID)).thenReturn(List.of(sampleDataset()));

    mockMvc.perform(get("/datasets").header("X-User-Id", USER_ID)).andExpect(status().isOk());

    verify(datasetService).listDatasets(USER_UUID);
  }

  @Test
  @DisplayName("GET /datasets/{datasetId} returns 200 for an existing dataset")
  void getDatasetById_existingDataset_returns200() throws Exception {
    when(datasetService.getDataset(DATASET_ID)).thenReturn(sampleDataset());

    mockMvc.perform(get("/datasets/{id}", DATASET_ID)).andExpect(status().isOk());

    verify(datasetService).getDataset(DATASET_ID);
  }
}
