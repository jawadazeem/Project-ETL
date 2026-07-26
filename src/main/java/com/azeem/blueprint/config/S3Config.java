/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

  @Value("${aws.s3.billing-bucket:cloud-billing}")
  private String billingBucketName;

  @Bean(name = "billingBucketName")
  public String billingBucketName() {
    return billingBucketName;
  }
}
