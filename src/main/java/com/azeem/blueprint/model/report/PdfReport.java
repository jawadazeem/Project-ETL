/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.report;

import java.time.Instant;
import java.util.UUID;

public record PdfReport(
    UUID id,
    UUID datasetId,
    UUID userId,
    String billingPeriod,
    String s3ObjectKey,
    Long fileSizeBytes,
    Instant generatedAt) {}
