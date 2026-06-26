/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.billing;

public enum CloudProvider {
  AWS,
  GCP,
  AZURE,
  OTHER;

  public static CloudProvider fromString(String value) {
    return CloudProvider.valueOf(value.toUpperCase());
  }
}
