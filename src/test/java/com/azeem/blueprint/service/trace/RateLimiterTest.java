/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  @Test
  @DisplayName("Allows requests up to the limit")
  void allowsRequestsUpToLimit() {
    RateLimiter limiter = new RateLimiter();
    for (int i = 0; i < 30; i++) {
      assertThat(limiter.tryAcquire()).isTrue();
    }
  }

  @Test
  @DisplayName("Rejects requests beyond the limit")
  void rejectsRequestsBeyondLimit() {
    RateLimiter limiter = new RateLimiter();
    for (int i = 0; i < 30; i++) {
      limiter.tryAcquire();
    }
    assertThat(limiter.tryAcquire()).isFalse();
  }
}
