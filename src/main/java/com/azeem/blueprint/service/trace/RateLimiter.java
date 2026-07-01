/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Simple sliding-window rate limiter for the Trace AI endpoint. Limits the total number of
 * requests across all users within a configurable time window.
 *
 * <p>This prevents runaway API quota consumption on the upstream Gemini model.
 */
@Component
public class RateLimiter {
  private static final int MAX_REQUESTS = 30;
  private static final long WINDOW_MS = 60_000;

  private final AtomicInteger count = new AtomicInteger(0);
  private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

  public boolean tryAcquire() {
    long now = System.currentTimeMillis();
    long start = windowStart.get();

    if (now - start > WINDOW_MS) {
      // Window expired — reset. CAS to avoid race where two threads both reset.
      if (windowStart.compareAndSet(start, now)) {
        count.set(1);
      } else {
        // Another thread already reset; just increment
        return count.incrementAndGet() <= MAX_REQUESTS;
      }
      return true;
    }

    return count.incrementAndGet() <= MAX_REQUESTS;
  }
}
