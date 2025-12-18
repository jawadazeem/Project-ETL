package com.azeem.billing.controller;

import com.azeem.billing.model.Alarm;
import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.service.AlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlarmController {
    private static final Logger log = LoggerFactory.getLogger(AlarmController.class);
    AlarmService service;

    public AlarmController(AlarmService service) {
        this.service = service;
    }

    @GetMapping("/alarms/{billingPeriod}")
    public List<Alarm> getAllAlarmsByPeriod(@PathVariable String billingPeriod) {
        log.info("GET /alarms/{} called to retrieve all alarms for {}.", billingPeriod, billingPeriod);
        return service.getAllAlarms(billingPeriod);
    }

    @GetMapping("/alarms/department/{billingPeriod}")
    public List<Alarm> getAllDepartmentAlarmsByPeriod(@PathVariable String billingPeriod) {
        log.info("GET /alarms/department/{} called to retrieve all department alarms.", billingPeriod);
        return service.getDepartmentAlarms(billingPeriod);
    }

    @GetMapping("/alarms/individual/{billingPeriod}")
    public List<Alarm> getAllIndividualAlarmsByPeriod(@PathVariable String billingPeriod) {
        log.info("GET /alarms/individual/{} called to retrieve all individual alarms.", billingPeriod);
        return service.getIndividualAlarms(billingPeriod);
    }

    @GetMapping("/alarms/account/{billingPeriod}")
    public List<Alarm> getAccountBudgetAlarm(@PathVariable String billingPeriod) {
        log.info("GET /alarms/account/{billingPeriod} called to retrieve if the account has exceeded its total budget.", billingPeriod);
        return service.getAccountAlarm(billingPeriod);
    }
}
