/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

import java.util.UUID;

public class CloudConnectionNotFoundException extends RuntimeException {
  public CloudConnectionNotFoundException(UUID connectionId) {
    super("Cloud connection not found: " + connectionId);
  }
}
