/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

import java.util.UUID;

public class PdfReportNotFoundException extends RuntimeException {
  public PdfReportNotFoundException(UUID reportId) {
    super("PDF report not found: " + reportId);
  }
}
