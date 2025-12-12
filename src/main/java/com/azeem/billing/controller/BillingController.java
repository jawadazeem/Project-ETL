package com.azeem.billing.controller;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import com.azeem.billing.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for billing data retrieval and summary generation.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>GET /records - Retrieve all billing records.</li>
 *   <li>GET /summary - Get aggregated billing summary.</li>
 *   <li>GET /records/state/{state} - Get billing records filtered by state.</li>
 *   <li>GET /top/{n} - Get top N billing records by total charge.</li>
 * </ul>
 */

@RestController
public class BillingController {
    private static final Logger log = LoggerFactory.getLogger(BillingController.class);
    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @GetMapping("/records")
    public List<BillingRecord> getAllRecords() {
        log.info("GET /records called to retrieve all billing records.");
        return service.getAllRecords();
    }

    @GetMapping("/summary")
    public BillingSummary getSummary() {
        log.info("GET /summary called to generate billing summary.");
        return service.generateSummary();
    }

    @GetMapping("/records/state/{state}")
    public List<BillingRecord> getRecordsByState(@PathVariable String state) {
        log.info("GET /records/state/{} called to retrieve records for state.", state);
        return service.getRecordsByState(state);
    }

    @GetMapping("/top/{n}")
    public List<BillingRecord> getTopN(@PathVariable int n) {
        log.info("GET /top/{} called to retrieve top N billing records by total charge.", n);
        return service.getTopNRecords(n);
    }

}
