/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.config.SecurityConfig;
import com.azeem.blueprint.model.trace.TraceRequest;
import com.azeem.blueprint.model.trace.TraceResponse;
import com.azeem.blueprint.service.trace.TraceService;
import com.azeem.blueprint.service.trace.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TraceController.class)
@Import(SecurityConfig.class)
@WithMockUser
class TraceControllerTest {
  private static final String DATASET_ID = "00000000-0000-0000-0000-000000000001";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  @MockitoBean private TraceService traceService;
  @MockitoBean private RateLimiter rateLimiter;

  @Test
  void shouldReturnChatResponse() throws Exception {
    when(rateLimiter.tryAcquire()).thenReturn(true);
    TraceRequest request = new TraceRequest("testing", "2026-01");

    when(traceService.ask(eq("testing"), any(UUID.class), eq("2026-01")))
        .thenReturn(new TraceResponse("test response", "test SQL", "test reasoning"));

    mockMvc
        .perform(
            post("/datasets/{datasetId}/trace", DATASET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(traceService).ask(eq(request.getPrompt()), any(UUID.class), eq(request.getPeriod()));
  }
}
