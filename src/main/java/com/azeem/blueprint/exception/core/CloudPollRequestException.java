/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class CloudPollRequestException extends RuntimeException {
  public CloudPollRequestException(String message) {
    super(message);
  }

  public CloudPollRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
