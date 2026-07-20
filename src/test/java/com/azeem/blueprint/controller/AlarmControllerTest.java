/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.etl.CsvExportService;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.billing.CloudProvider;
import com.azeem.blueprint.service.alarm.AlarmService;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

  @MockitoBean private AlarmService alarmService;
  @MockitoBean private CsvExportService csvExportService;

  @Test
  void shouldReturnAlarmsByBillingPeriod() throws Exception {
    when(alarmService.getAllAlarmsInDataset(any(UUID.class), eq(PERIOD)))
        .thenReturn(
            List.of(Alarm.provider(UUID.fromString(DATASET_ID), PERIOD, CloudProvider.AWS)));

    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}", DATASET_ID, PERIOD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].alarmScope").value("PROVIDER"))
        .andExpect(jsonPath("$[0].billingPeriod").value(PERIOD))
        .andExpect(jsonPath("$[0].cloudProvider").value("AWS"));

    verify(alarmService).getAllAlarmsInDataset(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldReturnAllProviderAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/provider", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getProviderAlarmsInDataset(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldReturnAllResourceAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/resource", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getResourceAlarmsInDataset(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldReturnAllAccountAlarmsByBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/account", DATASET_ID, PERIOD))
        .andExpect(status().isOk());

    verify(alarmService).getAccountAlarm(any(UUID.class), eq(PERIOD));
  }

  @Test
  void shouldExportAlarmsByBillingPeriod() throws Exception {
    List<Alarm> alarms = List.of(Alarm.accountHigh(UUID.fromString(DATASET_ID), PERIOD));
    when(alarmService.getAllAlarmsInDataset(any(UUID.class), eq(PERIOD))).thenReturn(alarms);
    doAnswer(
            invocation -> {
              OutputStream out = invocation.getArgument(1);
              out.write(
                  "Scope,Type,Severity\nACCOUNT,Total Account Budget Exceeded: HIGH,HIGH\n"
                      .getBytes(StandardCharsets.UTF_8));
              return null;
            })
        .when(csvExportService)
        .writeAlarms(eq(alarms), any(OutputStream.class));

    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}/export", DATASET_ID, PERIOD))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", "attachment; filename=\"alarms-" + PERIOD + ".csv\""))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ACCOUNT")));

    verify(alarmService).getAllAlarmsInDataset(any(UUID.class), eq(PERIOD));
    verify(csvExportService).writeAlarms(eq(alarms), any(OutputStream.class));
  }

  @Test
  void shouldRejectInvalidBillingPeriod() throws Exception {
    mockMvc
        .perform(get("/datasets/{datasetId}/alarms/{period}", DATASET_ID, "70-2070"))
        .andExpect(status().isBadRequest());
  }
}
