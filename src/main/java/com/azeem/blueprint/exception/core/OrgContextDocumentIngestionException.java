/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

public class OrgContextDocumentIngestionException extends RuntimeException {
  public OrgContextDocumentIngestionException(String message) {
    super(message);
  }

  public OrgContextDocumentIngestionException(String message, Throwable cause) {
    super(message, cause);
  }

  public OrgContextDocumentIngestionException() {}

  public OrgContextDocumentIngestionException(Throwable cause) {
    super(cause);
  }
}
