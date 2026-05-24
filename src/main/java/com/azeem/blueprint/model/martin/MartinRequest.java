/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.martin;

import jakarta.validation.constraints.NotBlank;

/** Martin Request DTO */
public class MartinRequest {

  @NotBlank(message = "Prompt must not be blank")
  private String prompt;

  @NotBlank(message = "Billing period must not be blank")
  private String period;

  public MartinRequest() {}

  public MartinRequest(String prompt, String period) {
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
