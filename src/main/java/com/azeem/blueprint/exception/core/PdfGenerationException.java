/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class PdfGenerationException extends RuntimeException {
  public PdfGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
