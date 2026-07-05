package com.azeem.blueprint.model.trace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
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

  @NotNull(message = "Owner ID must not be null")
  private UUID ownerUserId;

  public TraceRequest(String prompt, UUID ownerUserId, String currentPeriod) {
    this.prompt = prompt;
    this.ownerUserId = ownerUserId;
    this.currentPeriod = currentPeriod;
  }
}
