/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.trace;

import jakarta.validation.constraints.NotBlank;

/** Trace Request DTO */
public class TraceRequest {

  @NotBlank(message = "Prompt must not be blank")
  private String prompt;

  @NotBlank(message = "Billing period must not be blank")
  private String period;

  public TraceRequest() {}

  public TraceRequest(String prompt, String period) {
    this.prompt = prompt;
    this.period = period;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }
}
