package com.azeem.billing.controller;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import com.azeem.billing.service.BillingService;
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
    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @GetMapping("/records")
    public List<BillingRecord> getAllRecords() {
        return service.getAllRecords();
    }

    @GetMapping("/summary")
    public BillingSummary getSummary() {
        return service.generateSummary();
    }

    @GetMapping("/records/state/{state}")
    public List<BillingRecord> getRecordsByState(@PathVariable String state) {
        return service.getAllRecords().stream().filter(r -> r.state().equalsIgnoreCase(state)).toList();
    }

    @GetMapping("/top/{n}")
    public List<BillingRecord> getTopN(@PathVariable int n) {
        return service.getAllRecords().stream().sorted((a, b) -> Double.compare(b.totalCharge(), a.totalCharge())).limit(n).toList();
    }

}
