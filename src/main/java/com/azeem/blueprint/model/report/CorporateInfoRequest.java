/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.report;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CorporateInfoRequest(
    @NotBlank(message = "Company name must not be blank") String companyName,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String zipCode,
    String country,
    String phone,
    @Email(message = "Email must be valid") String email,
    String logoUrl) {}
