package com.azeem.billing.service;

import com.azeem.billing.config.AlarmConfig;
import com.azeem.billing.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

import static com.azeem.billing.model.AlarmSeverity.LOW;

/**
 * <p>Input: List<BillingRecord> (scoped by billingPeriod)</p>
 * <p>Output: List<Alarm> (NOT persisted)</p>
 * <p>No repositories</p>
 * <p>No side effects</p>
 */
@Service
public class AlarmDetectionService {
    private final AlarmConfig alarmConfig;

    public AlarmDetectionService(AlarmConfig alarmConfig) {
        this.alarmConfig = alarmConfig;
    }

    public List<Alarm> detectAlarms(List<BillingRecord> records, String billingPeriod) {
        List<Alarm> alarms = getDepartmentsOverLimit(records, billingPeriod);
        alarms.addAll(getIndividualChargesOverLimit(records, billingPeriod));
        alarms.addAll(getGrandTotalOverLimit(records, billingPeriod));
        return alarms;
    }


    private List<Alarm> getDepartmentsOverLimit(List<BillingRecord> records, String billingPeriod) {
        List<Alarm> alarms = new ArrayList<>();
        Map<String, Double> totals = new HashMap<>();

        for (BillingRecord r : records) {
            if (r.department().equals("Engineering")) {
                totals.merge("Engineering", r.totalCharge(), Double::sum);
            } else if (r.department().equals("Finance")) {
                totals.merge("Finance", r.totalCharge(), Double::sum);
            } else if (r.department().equals("HR")) {
                totals.merge("HR", r.totalCharge(), Double::sum);
            } else if (r.department().equals("IT")) {
                totals.merge("IT", r.totalCharge(), Double::sum);
            } else if (r.department().equals("Legal")) {
                totals.merge("Legal", r.totalCharge(), Double::sum);
            } else if (r.department().equals("Marketing")) {
                totals.merge("Marketing", r.totalCharge(), Double::sum);
            } else if (r.department().equals("Operations")) {
                totals.merge("Operations", r.totalCharge(), Double::sum);
            } else if (r.department().equals("Sales")) {
                totals.merge("Sales", r.totalCharge(), Double::sum);
            } else if (r.department().equals("Support")) {
                totals.merge("Support", r.totalCharge(), Double::sum);
            }
        }

        double deptLimit = alarmConfig.getDepartment().getMonthlyLimit();

        if (totals.getOrDefault("Engineering", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Engineering department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.ENGINEERING);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("Finance", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Finance department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.FINANCE);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("HR", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "HR department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.HR);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("IT", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "IT department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.IT);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("Legal", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Legal department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.LEGAL);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("Marketing", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Marketing department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.MARKETING);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("Operations", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Operations department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.OPERATIONS);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("Sales", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Sales department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.SALES);
            alarms.add(alarm);
        }
        if (totals.getOrDefault("Support", 0.0) > deptLimit) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Support department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.SUPPORT);
            alarms.add(alarm);
        }
        return alarms;
    }

    private List<Alarm> getIndividualChargesOverLimit(List<BillingRecord> records, String billingPeriod) {
        List<Alarm> alarms = new ArrayList<>();
        double low = alarmConfig.getIndividual().getLow();
        double medium = alarmConfig.getIndividual().getMedium();
        double high = alarmConfig.getIndividual().getHigh();

        for (BillingRecord r : records) {
            if (r.totalCharge() >= low && r.totalCharge() < medium) {
                Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.INDIVIDUAL, billingPeriod,
                        "Individual Charge Limit Exceeded",
                        AlarmSeverity.LOW,
                        "Exceeds Charge Limit: LOW", Instant.now(),
                        r.employeeId(), r.phoneNumber(), null);
                alarms.add(alarm);
            } else if (r.totalCharge() >= medium && r.totalCharge() < high) {
                Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.INDIVIDUAL, billingPeriod,
                        "Individual Charge Limit Exceeded",
                        AlarmSeverity.MEDIUM,
                        "Slightly exceeds Charge Limit: MEDIUM", Instant.now(),
                        r.employeeId(), r.phoneNumber(), null);
                alarms.add(alarm);
            } else if (r.totalCharge() > high) {
                Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.INDIVIDUAL, billingPeriod,
                        "Individual Charge Limit Exceeded",
                        AlarmSeverity.HIGH,
                        "Significantly exceeds Charge Limit (TAKE ACTION)", Instant.now(),
                        r.employeeId(), r.phoneNumber(), null);
                alarms.add(alarm);
            }
        }
        return alarms;
    }

    private List<Alarm> getGrandTotalOverLimit(List<BillingRecord> records, String billingPeriod) {
        double grandTotal = 0;
        double accountLow = alarmConfig.getAccount().getLow();
        double accountHigh = alarmConfig.getAccount().getHigh();

        for (BillingRecord r : records) {
            grandTotal += r.totalCharge();
        }

        if (grandTotal > accountLow && grandTotal < accountHigh) {
            Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.ACCOUNT, billingPeriod,
                    "Total Account Budget Exceeded: LOW",
                    AlarmSeverity.LOW,
                    "Your account's telecom bill has slightly exceeded its monthly budget.", Instant.now(),
                    null, null, null);
            List<Alarm> alarms = new ArrayList<>();
            alarms.add(alarm);
            return alarms;
        }
        Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.ACCOUNT, billingPeriod,
                "Total Account Budget Exceeded: HIGH",
                AlarmSeverity.HIGH,
                "Your account's telecom bill has significantly exceeded its monthly budget.", Instant.now(),
                null, null, null);
        List<Alarm> alarms = new ArrayList<>();
        alarms.add(alarm);
        return alarms;
    }
}
