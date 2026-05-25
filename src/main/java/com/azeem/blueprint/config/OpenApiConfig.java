/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI blueprintOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Blueprint Billing API")
                .description("Telecom Billing Intelligence Platform")
                .version("1.0.0"));
  }
}
