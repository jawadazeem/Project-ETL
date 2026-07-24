/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.cloudconnection;

import java.time.Instant;
import java.util.UUID;

public record CloudConnection(
    UUID id,
    UUID ownerUserId,
    String provider,
    String displayName,
    String bucketName,
    String region,
    CloudConnectionStatus status,
    PollFrequency pollFrequency,
    Instant lastPolledAt,
    Instant createdAt,
    Instant updatedAt) {}
