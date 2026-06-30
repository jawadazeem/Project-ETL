/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.trace;

/**
 * Trace's SQL Response DTO
 *
 * <p>SQL script and reasoning are including in {@link TraceResponse}
 */
public class SqlResponse {
  private String sql;
  private String reasoning;

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public String getReasoning() {
    return reasoning;
  }

  public void setReasoning(String reasoning) {
    this.reasoning = reasoning;
  }
}
