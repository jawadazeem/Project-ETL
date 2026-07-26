/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class CloudConnectionDecryptionException extends RuntimeException {
  public CloudConnectionDecryptionException(String message) {
    super(message);
  }

  public CloudConnectionDecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
