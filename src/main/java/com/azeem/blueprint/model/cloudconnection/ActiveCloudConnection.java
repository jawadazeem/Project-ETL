/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.cloudconnection;

import java.util.Map;
import java.util.UUID;

public record ActiveCloudConnection(
    UUID id,
    UUID ownerUserId,
    String provider,
    String bucketName,
    String region,
    Map<String, String> credentials) {}
