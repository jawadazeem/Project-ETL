/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
public class EventBridgeConfig {
  @Bean
  public EventBridgeClient eventBridgeClient(
      @Value("${AWS_EVENTBRIDGE_ENDPOINT:http://localstack:4566}") String endpoint,
      @Value("${AWS_REGION:us-east-1}") String region) {

    return EventBridgeClient.builder()
        .region(Region.of(region))
        .endpointOverride(URI.create(endpoint))
        .build();
  }
}
