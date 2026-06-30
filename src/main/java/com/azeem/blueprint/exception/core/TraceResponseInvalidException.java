/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class TraceResponseInvalidException extends RuntimeException {
  public TraceResponseInvalidException(String message, Throwable cause) {
    super(message, cause);
  }

  public TraceResponseInvalidException(String message) {
    super(message);
  }
}
