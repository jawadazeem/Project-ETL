/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager manager =
        new CaffeineCacheManager("billingSummaries", "billingPeriods", "providers", "alarms");
    manager.setCaffeine(
        Caffeine.newBuilder().maximumSize(500).expireAfterWrite(Duration.ofMinutes(10)));
    return manager;
  }
}
