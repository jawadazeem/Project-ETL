/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.cloudconnection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record CloudConnectionRequest(
    @NotBlank String provider,
    @NotBlank String displayName,
    @NotBlank String bucketName,
    String region,
    @NotEmpty Map<String, String> credentials) {}
