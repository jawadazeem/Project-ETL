package com.azeem.billing.controller;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import com.azeem.billing.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO: Add pagination to endpoints returning lists of records to handle large datasets efficiently.
/**
 * REST controller exposing endpoints for billing data retrieval and summary generation.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>GET /records - Retrieve all billing records.</li>
 *   <li>GET /summary - Get aggregated billing summary.</li>
 *   <li>GET /records/department/{department} - Get billing records filtered by department.</li>
 *   <li>GET /top/{n} - Get top N billing records by total charge.</li>
 *   <li>GET /departments - List all unique departments.</li>
 *   <li>GET /periods - List all available billing periods.</li>
 *   <li>GET /records/period/{billingPeriod} - Get billing records for
 *   specified billing period.</li>
 *   <li>GET /summary/period/{billingPeriod} - Get billing summary for
 *   specified billing period.</li>
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

    @GetMapping("/records/department/{department}")
    public List<BillingRecord> getRecordsByDepartment(@PathVariable String department) {
        log.info("GET /records/department/{} called to retrieve records for department.", department);
        return service.getRecordsByDepartment(department);
    }

    @GetMapping("/top/{n}")
    public List<BillingRecord> getTopN(@PathVariable int n) {
        log.info("GET /top/{} called to retrieve top N billing records by total charge.", n);
        return service.getTopNRecords(n);
    }

    @GetMapping("/departments")
    public List<String> getDepartments() {
        log.info("GET /departments called.");
        return service.getAllRecords().stream()
                .map(BillingRecord::department)
                .distinct()
                .sorted()
                .toList();
    }

    @GetMapping("/periods")
    public List<String> getBillingPeriods() {
        log.info("GET /periods called.");
        return service.getAvailableBillingPeriods();
    }

    @GetMapping("/records/period/{billingPeriod}")
    public List<BillingRecord> getRecordsByPeriod(@PathVariable String billingPeriod) {
        log.info("GET /records/period/{} called.", billingPeriod);
        return service.getRecordsByPeriod(billingPeriod);
    }

    @GetMapping("/summary/period/{billingPeriod}")
    public BillingSummary getSummaryByPeriod(@PathVariable String billingPeriod) {
        log.info("GET /summary/period/{} called.", billingPeriod);
        return service.generateSummaryForPeriod(billingPeriod);
    }
}
