/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller.preference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.service.preference.AlarmPreferenceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlarmPreferenceController.class)
@WithMockUser
class AlarmPreferenceControllerTest {
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AlarmPreferenceService alarmPreferenceService;

  @Test
  void shouldReturnCurrentUserPreference() throws Exception {
    when(alarmPreferenceService.getPreference(USER_ID)).thenReturn(preference());

    mockMvc
        .perform(get("/preferences/alarm-threshold/me").header("X-User-Id", USER_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider.monthlyLimit").value(25000));

    verify(alarmPreferenceService).getPreference(USER_ID);
  }

  @Test
  void shouldSaveCurrentUserPreferenceWithoutRecomputing() throws Exception {
    when(alarmPreferenceService.savePreference(eq(USER_ID), any())).thenReturn(preference());

    mockMvc
        .perform(
            put("/preferences/alarm-threshold/me")
                .header("X-User-Id", USER_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferenceJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.account.high").value(750000));

    verify(alarmPreferenceService).savePreference(eq(USER_ID), any());
  }

  @Test
  void shouldUpdateCurrentUserPreferenceAndRecomputeUserAlarms() throws Exception {
    when(alarmPreferenceService.updatePreferenceAndRecompute(eq(USER_ID), any()))
        .thenReturn(preference());

    mockMvc
        .perform(
            put("/preferences/alarm-threshold/me/recompute")
                .header("X-User-Id", USER_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferenceJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.account.high").value(750000));

    verify(alarmPreferenceService).updatePreferenceAndRecompute(eq(USER_ID), any());
  }

  @Test
  void shouldCreateCurrentUserPreferenceFromHeaderUser() throws Exception {
    when(alarmPreferenceService.savePreference(eq(USER_ID), any())).thenReturn(preference());

    mockMvc
        .perform(
            post("/preferences/alarm-threshold/me")
                .header("X-User-Id", USER_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "ownerUserId": "99999999-9999-9999-9999-999999999999",
                      "provider": {"monthlyLimit": 25000},
                      "individual": {"low": 1000, "medium": 2000, "high": 5000},
                      "account": {"low": 500000, "high": 750000}
                    }
                    """))
        .andExpect(status().isOk());

    verify(alarmPreferenceService).savePreference(eq(USER_ID), any());
  }

  private String preferenceJson() {
    return """
        {
          "provider": {"monthlyLimit": 25000},
          "individual": {"low": 1000, "medium": 2000, "high": 5000},
          "account": {"low": 500000, "high": 750000}
        }
        """;
  }

  private AlarmThresholdPreference preference() {
    return new AlarmThresholdPreference(
        null,
        USER_ID,
        new AlarmThresholdPreference.Provider(25000),
        new AlarmThresholdPreference.Individual(1000, 2000, 5000),
        new AlarmThresholdPreference.Account(500000, 750000));
  }
}
