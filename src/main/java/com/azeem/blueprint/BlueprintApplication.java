/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// TODO: A major design flaw in this application as a whole, that I am only learning now,
//  is that we need more than just a domain model and a data model. We need:
//  [ Client ] ---> ( Request DTO ) ---> [ API Controller ] ---> ( Domain Model / Logic )
@SpringBootApplication
@ConfigurationPropertiesScan("com.azeem.blueprint.config")
public class BlueprintApplication {
  public static void main(String[] args) {
    SpringApplication.run(BlueprintApplication.class, args);
  }
}
