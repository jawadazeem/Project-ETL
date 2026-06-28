/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.azeem.blueprint.config")
public class BlueprintApplication {
  public static void main(String[] args) {
    SpringApplication.run(BlueprintApplication.class, args);
  }
}
