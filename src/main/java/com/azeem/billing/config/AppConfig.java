package com.azeem.billing.config;

import com.azeem.billing.etl.BillParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public BillParser billParser() {
        // Load from resources later; hardcoded now
        return new BillParser("src/main/resources/senate_billing_2025.csv");
    }
}
