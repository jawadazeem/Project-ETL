/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.exception.core;

/** Exception thrown when no billing records are found for a given cloud provider. */
public class CloudProviderNotFoundException extends RuntimeException {

  public CloudProviderNotFoundException(String cloudProvider) {
    super("No billing records found for cloud provider: " + cloudProvider);
  }
}