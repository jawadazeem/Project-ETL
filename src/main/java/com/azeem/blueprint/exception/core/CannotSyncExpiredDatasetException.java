/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

/**
 * Thrown when there is an attempt to sync the billing data of a dataset that has expired.
 *
 * <p>In this context, an expired dataset is one which belongs to a period that reflects a billing
 * cycle prior to the current month's
 */
public class CannotSyncExpiredDatasetException extends RuntimeException {
  public CannotSyncExpiredDatasetException(String message) {
    super(message);
  }
}
