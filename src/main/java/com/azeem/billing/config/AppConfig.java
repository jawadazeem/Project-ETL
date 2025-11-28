package com.azeem.billing.config;

import com.azeem.billing.etl.BillParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class to define application beans.
 * Currently, it defines the BillParser bean.
 */

@Configuration
public class AppConfig {

    @Bean
    public BillParser billParser() {
        // TODO: Add upload capabilities
        return new BillParser("src/main/resources/senate_billing_2025.csv");
    }
}
