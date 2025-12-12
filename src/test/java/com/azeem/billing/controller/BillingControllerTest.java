package com.azeem.billing.controller;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import com.azeem.billing.service.BillingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillingController.class)
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingService service; // Spring injects the mock defined below

    @TestConfiguration
    static class MockConfig {

        @Bean
        BillingService billingService() {
            return Mockito.mock(BillingService.class);
        }
    }

    @Test
    void testSummaryEndpoint() throws Exception {
        BillingSummary summary = new BillingSummary();
        summary.setTotalRecords(5);
        summary.setTotalCharges(300.0);

        // Define mock behavior
        when(service.generateSummary()).thenReturn(summary);

        // Perform + assert JSON response
        mockMvc.perform(get("/summary"))
                .andExpect(status().isOk()) // HTTP status must be ok
                .andExpect(jsonPath("$.totalRecords").value(5))
                .andExpect(jsonPath("$.totalCharges").value(300.0));
    }

    @Test
    void testRecordsEndpoint() throws Exception {
        List<BillingRecord> fakeRecords = List.of(
                new BillingRecord("Account1", "S1", "CA", "555-1234", "2025-Jan",
                        100, 1.5, 20, 50.0)
        );

        // Define mock behavior
        when(service.getAllRecords()).thenReturn(fakeRecords);

        // Perform + assert JSON response
        mockMvc.perform(get("/records"))
                .andExpect(status().isOk()) // HTTP status must be ok
                .andExpect(jsonPath("$[0].accountName").value("Account1"))
                .andExpect(jsonPath("$[0].state").value("CA"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testFilterByStateEndpoint() throws Exception {
        List<BillingRecord> fakeRecords = List.of(
                new BillingRecord("Account1", "S1", "CA", "555-1234", "2025-Jan",
                        100, 5.5, 20, 150.0),
                new BillingRecord("Account2", "S2", "VA", "111-1234", "2025-Jan",
                        300, 2.5, 10, 70.0),
                new BillingRecord("Account3", "S3", "MD", "222-1234", "2025-Jan",
                        150, 1.5, 30, 10.0)
        );

        when(service.getRecordsByState("VA")).thenReturn(fakeRecords.subList(1,2));

        // call GET /records/state/VA
        mockMvc.perform(get ("/records/state/VA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountName").value("Account2"))
                .andExpect(jsonPath("$[0].state").value("VA"));
    }
}
