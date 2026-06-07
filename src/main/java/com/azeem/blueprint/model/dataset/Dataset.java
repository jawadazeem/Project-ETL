/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.dataset;

import java.time.Instant;
import java.util.UUID;

public record Dataset(
    UUID id,
    UUID ownerUserId,
    String billingPeriod,
    String sourceFilename,
    String s3ObjectKey,
    Instant uploadedAt,
    String status,
    boolean archived) {}
