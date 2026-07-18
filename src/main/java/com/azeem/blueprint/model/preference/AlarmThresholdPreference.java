/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.preference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record AlarmThresholdPreference(
    UUID id,
    UUID ownerUserId,
    @NotNull @Valid Provider provider,
    @NotNull @Valid Individual individual,
    @NotNull @Valid Account account) {

  public record Provider(@PositiveOrZero double monthlyLimit) {}

  public record Individual(
      @PositiveOrZero double low, @PositiveOrZero double medium, @PositiveOrZero double high) {
    @AssertTrue(message = "Individual thresholds must be ordered low <= medium <= high")
    public boolean isOrdered() {
      return low <= medium && medium <= high;
    }
  }

  public record Account(@PositiveOrZero double low, @PositiveOrZero double high) {
    @AssertTrue(message = "Account thresholds must be ordered low <= high")
    public boolean isOrdered() {
      return low <= high;
    }
  }
}
