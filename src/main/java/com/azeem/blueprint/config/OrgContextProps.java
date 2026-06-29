/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "org-context")
public class OrgContextProps {
  private String bucketName;
  private int maxFiles;

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public int getMaxFiles() {
    return maxFiles;
  }

  public void setMaxFiles(int maxFiles) {
    this.maxFiles = maxFiles;
  }
}
