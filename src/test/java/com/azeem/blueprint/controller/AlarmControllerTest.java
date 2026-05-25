/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.service.alarm.AlarmService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlarmController.class)
@WithMockUser
class AlarmControllerTest {
  private static final String DATASET_ID = "00000000-0000-0000-0000-000000000001";
  private static final String PERIOD = "2026-01";

  @Autowired private MockMvc mockMvc;

  @MockitoBean AlarmService alarmService;
  @MockitoBean com.azeem.blueprint.etl.CsvExportService csvExportService;

  @Test
  void shouldReturnAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getAllAlarmsInDataset(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldReturnAllDepartmentAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/department", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getDepartmentAlarmsInDataset(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldReturnAllIndividualAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/individual", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getIndividualAlarmsInDataset(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldReturnAllAccountAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/account", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getAccountAlarm(any(UUID.class), eq(PERIOD));
  }
}
