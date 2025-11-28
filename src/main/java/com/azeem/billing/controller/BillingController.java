package com.azeem.billing.controller;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.model.BillingSummary;
import com.azeem.billing.service.BillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
