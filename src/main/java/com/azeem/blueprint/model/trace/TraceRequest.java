package com.azeem.blueprint.model.trace;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Trace Request DTO */
@Getter
@Setter
@NoArgsConstructor
public class TraceRequest {

  @NotBlank(message = "Prompt must not be blank")
  private String prompt;

  @NotBlank(message = "Billing period must not be blank")
  private String currentPeriod;

  public TraceRequest(String prompt, String currentPeriod) {
    this.prompt = prompt;
    this.currentPeriod = currentPeriod;
  }
}
