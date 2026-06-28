/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class MartinResponseInvalidException extends RuntimeException {
  public MartinResponseInvalidException(String message, Throwable cause) {
    super(message, cause);
  }

  public MartinResponseInvalidException(String message) {
    super(message);
  }
}
