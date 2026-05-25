/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.report;

import java.time.Instant;
import java.util.UUID;

public record CorporateInfo(
    UUID id,
    UUID userId,
    String companyName,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String zipCode,
    String country,
    String phone,
    String email,
    String logoUrl,
    Instant createdAt,
    Instant updatedAt) {}
