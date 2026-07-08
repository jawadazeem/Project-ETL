/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.trace;

public record PromptRoute(boolean canRespond, TaskType taskType, String reason) {
  public static PromptRoute accepted(TaskType taskType, String reason) {
    return new PromptRoute(true, taskType, reason);
  }

  public static PromptRoute rejected(String reason) {
    return new PromptRoute(false, TaskType.GENERAL, reason);
  }
}
