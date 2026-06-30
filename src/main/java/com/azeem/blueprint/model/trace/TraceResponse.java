/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.trace;

/** Trace Response DTO */
public class TraceResponse {
  public String answer;
  public String sql;
  public String reasoning;

  public TraceResponse(String answer, String sql, String reasoning) {
    this.answer = answer;
    this.sql = sql;
    this.reasoning = reasoning;
  }
}
