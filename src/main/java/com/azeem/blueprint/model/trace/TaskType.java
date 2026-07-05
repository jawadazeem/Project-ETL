/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.trace;

public enum TaskType {
  COST_OPTIMIZATION("cost-optimization-recommendations.md"),
  AUDIT("audit.md"),
  POLICY_AND_CONTRACT_FIT("policy-and-contract-fit.md"),
  EXPLAIN_SPEND_INCREASE("explain-spend-increase.md"),
  EXECUTIVE_SUMMARY("executive-summary.md"),
  GENERAL("general.md");

  private final String filename;

  TaskType(String filename) {
    this.filename = filename;
  }

  public String getFilename() {
    return filename;
  }

  public static TaskType fromFilename(String filename) {
    if (filename == null) {
      return null;
    }

    for (TaskType t : values()) {
      if (t.filename.equals(filename)) {
        return t;
      }
    }
    throw new IllegalArgumentException("A Task for the given filename does not exist: " + filename);
  }
}
