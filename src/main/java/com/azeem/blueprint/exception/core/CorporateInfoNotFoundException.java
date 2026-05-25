/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

import java.util.UUID;

public class CorporateInfoNotFoundException extends RuntimeException {
  public CorporateInfoNotFoundException(UUID userId) {
    super("Corporate info not found for user: " + userId);
  }
}
