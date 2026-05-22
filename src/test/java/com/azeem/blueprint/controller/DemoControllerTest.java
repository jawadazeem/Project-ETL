/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azeem.blueprint.config.SecurityConfig;
import com.azeem.blueprint.service.dataset.demo.DemoDatasetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DemoController.class)
@Import(SecurityConfig.class)
@WithMockUser
class DemoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DemoDatasetLoader demoDatasetLoader;

  @Test
  @DisplayName("POST /demo-dataset returns 200 and triggers demo data loading")
  void loadDemoData_returns200AndDelegates() throws Exception {
    mockMvc
        .perform(post("/demo-dataset"))
        .andExpect(status().isOk())
        .andExpect(content().string("Demo data loaded. You can now use the analytics endpoints."));

    verify(demoDatasetLoader).loadDemoData();
  }
}
