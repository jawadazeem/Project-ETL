/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "billing-reader")
@lombok.Data
public class BillingReaderConfig {
  private boolean hasHeader;
  private int batchSize;

  public boolean hasHeader() {
    return hasHeader;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void setHasHeader(boolean hasHeader) {
    this.hasHeader = hasHeader;
  }
}
