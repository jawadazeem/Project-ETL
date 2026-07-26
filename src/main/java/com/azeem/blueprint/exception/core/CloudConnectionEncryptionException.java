/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class CloudConnectionEncryptionException extends RuntimeException {
  public CloudConnectionEncryptionException(String message) {
    super(message);
  }

  public CloudConnectionEncryptionException(String message, Throwable cause) {
    super(message, cause);
  }

  public CloudConnectionEncryptionException(Throwable cause) {
    super(cause);
  }

  public CloudConnectionEncryptionException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public CloudConnectionEncryptionException() {}
}
