/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.config.SecurityConfig;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.CorporateInfoRequest;
import com.azeem.blueprint.service.report.CorporateInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CorporateInfoController.class)
@Import(SecurityConfig.class)
@WithMockUser
class CorporateInfoControllerTest {

  private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  @MockitoBean private CorporateInfoService corporateInfoService;

  @Test
  @DisplayName("GET returns 200 when corporate info exists")
  void shouldReturn200WhenCorporateInfoExists() throws Exception {
    CorporateInfo info = makeCorporateInfo();
    when(corporateInfoService.getCorporateInfo(UUID.fromString(USER_ID)))
        .thenReturn(Optional.of(info));

    mockMvc
        .perform(get("/users/{userId}/corporate-info", USER_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyName").value("Acme Corp"));
  }

  @Test
  @DisplayName("GET returns 404 when no corporate info exists")
  void shouldReturn404WhenNoCorporateInfo() throws Exception {
    when(corporateInfoService.getCorporateInfo(UUID.fromString(USER_ID)))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(get("/users/{userId}/corporate-info", USER_ID))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("PUT returns 200 on valid upsert")
  void shouldReturn200OnUpsert() throws Exception {
    CorporateInfoRequest request =
        new CorporateInfoRequest("Acme Corp", null, null, null, null, null, null, null, null, null);
    CorporateInfo saved = makeCorporateInfo();

    when(corporateInfoService.upsertCorporateInfo(eq(UUID.fromString(USER_ID)), any()))
        .thenReturn(saved);

    mockMvc
        .perform(
            put("/users/{userId}/corporate-info", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyName").value("Acme Corp"));
  }

  @Test
  @DisplayName("PUT returns 400 when company name is blank")
  void shouldReturn400OnInvalidRequest() throws Exception {
    CorporateInfoRequest request =
        new CorporateInfoRequest("", null, null, null, null, null, null, null, null, null);

    mockMvc
        .perform(
            put("/users/{userId}/corporate-info", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  private CorporateInfo makeCorporateInfo() {
    return new CorporateInfo(
        UUID.randomUUID(),
        UUID.fromString(USER_ID),
        "Acme Corp",
        "123 Main St",
        null,
        "Springfield",
        "IL",
        "62701",
        "US",
        "555-0100",
        "info@acme.com",
        null,
        Instant.now(),
        Instant.now());
  }
}
