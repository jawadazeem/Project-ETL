/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class BillingDataIngestionException extends RuntimeException {
  public BillingDataIngestionException(String message, Throwable cause) {
    super(message, cause);
  }

  public BillingDataIngestionException(String message) {
    super(message);
  }
}
