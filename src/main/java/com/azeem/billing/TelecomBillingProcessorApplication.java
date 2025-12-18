package com.azeem.billing;

import com.azeem.billing.service.BillingIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@SpringBootApplication
public class TelecomBillingProcessorApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TelecomBillingProcessorApplication.class);

        app.addListeners(event -> {
            if (event instanceof ApplicationReadyEvent readyEvent) {
                readyEvent.getApplicationContext()
                        .getBean(BillingIngestionService.class)
                        .ingestData("2025-01","src/main/resources/azeemcom_telecom_usage_2025_01.csv");
            }
        });

        app.run(args);
    }
}
