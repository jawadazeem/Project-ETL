package com.azeem.billing.service;

import com.azeem.billing.entity.AlarmEntity;
import com.azeem.billing.mapper.AlarmMapper;
import com.azeem.billing.mapper.BillingRecordMapper;
import com.azeem.billing.model.*;
import com.azeem.billing.repository.AlarmRepository;
import com.azeem.billing.repository.BillingRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.azeem.billing.model.AlarmSeverity.*;

@Service
public class AlarmService {
    //TODO: Implement historical average change checks

    AlarmRepository alarmRepository;
    BillingRecordRepository billingRecordRepository;
    AlarmMapper alarmMapper = new AlarmMapper();
    BillingRecordMapper billingMapper = new BillingRecordMapper();

    public AlarmService(AlarmRepository alarmRepository, BillingRecordRepository billingRecordRepository) {
        this.alarmRepository = alarmRepository;
        this.billingRecordRepository = billingRecordRepository;
    }

    public List<Alarm> getAllAlarmsByPeriod(String billingPeriod) {
        return alarmRepository.findByBillingPeriod(billingPeriod).stream().map(alarmMapper::mapToDomain).toList();
    }

    public List<Alarm> getDepartmentsOverLimit(String billingPeriod) {
        List<BillingRecord> records = billingRecordRepository.findByBillingPeriod(billingPeriod).stream().map(billingMapper::mapToDomain).toList();
        List<Alarm> alarms = new ArrayList<>();
        double engineeringTotal = 0;
        double financeTotal = 0;
        double hRTotal = 0;
        double iTTotal = 0;
        double legalTotal = 0;
        double marketingTotal = 0;
        double operationsTotal = 0;
        double salesTotal = 0;
        double supportTotal = 0;

        for (BillingRecord r : records) {
            if (r.department().equals("Engineering")) {
                engineeringTotal += r.totalCharge();
            } else if (r.department().equals("Finance")) {
                financeTotal += r.totalCharge();
            } else if (r.department().equals("HR")) {
                hRTotal += r.totalCharge();
            } else if (r.department().equals("IT")) {
                iTTotal += r.totalCharge();
            } else if (r.department().equals("Legal")) {
                legalTotal += r.totalCharge();
            } else if (r.department().equals("Marketing")) {
                marketingTotal += r.totalCharge();
            } else if (r.department().equals("Operations")) {
                operationsTotal += r.totalCharge();
            } else if (r.department().equals("Sales")) {
                salesTotal += r.totalCharge();
            } else if (r.department().equals("Support")) {
                supportTotal += r.totalCharge();
            }
        }

        // Threshold for department total charges is 1000, this can be changed
        if (engineeringTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Engineering department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.ENGINEERING);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (financeTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Finance department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.FINANCE);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (hRTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "HR department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.HR);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (iTTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "IT department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.IT);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (legalTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Legal department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.LEGAL);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (marketingTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Marketing department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.MARKETING);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (operationsTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Operations department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.OPERATIONS);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (salesTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Sales department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.SALES);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        if (supportTotal > 1000) {
            Alarm alarm = new Alarm(UUID.randomUUID(),
                    AlarmScope.DEPARTMENT,
                    billingPeriod,
                    "Department Charge Exceeded",
                    LOW, "Support department Exceeds Charge Limit",
                    Instant.now(), null, null, Department.SUPPORT);
            AlarmEntity entity = alarmMapper.mapToEntity(alarm);
            alarmRepository.save(entity);
            alarms.add(alarm);
        }
        return alarms;
    }

    public List<Alarm> getIndividualChargesOverLimit(String billingPeriod) {
        List<BillingRecord> records = billingRecordRepository.findByBillingPeriod(billingPeriod).stream().map(billingMapper::mapToDomain).toList();
        List<Alarm> alarms = new ArrayList<>();

        for (BillingRecord r : records) {
            if (r.totalCharge() >= 150 && r.totalCharge() < 300) {
                Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.INDIVIDUAL, billingPeriod,
                        "Individual Charge Limit Exceeded",
                         AlarmSeverity.LOW,
                        "Exceeds Charge Limit: LOW", Instant.now(),
                        r.employeeId(), r.phoneNumber(), null);
                AlarmEntity entity = alarmMapper.mapToEntity(alarm);
                alarmRepository.save(entity);
                alarms.add(alarm);
            } else if (r.totalCharge() >= 300 && r.totalCharge() < 500) {
                Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.INDIVIDUAL, billingPeriod,
                        "Individual Charge Limit Exceeded",
                        AlarmSeverity.MEDIUM,
                        "Exceeds Charge Limit: LOW", Instant.now(),
                        r.employeeId(), r.phoneNumber(), null);
                AlarmEntity entity = alarmMapper.mapToEntity(alarm);
                alarmRepository.save(entity);
                alarms.add(alarm);
            } else if (r.totalCharge() > 500) {
                Alarm alarm = new Alarm(UUID.randomUUID(), AlarmScope.INDIVIDUAL, billingPeriod,
                        "Individual Charge Limit Exceeded",
                        AlarmSeverity.HIGH,
                        "Exceeds Charge Limit: LOW", Instant.now(),
                        r.employeeId(), r.phoneNumber(), null);
                AlarmEntity entity = alarmMapper.mapToEntity(alarm);
                alarmRepository.save(entity);
                alarms.add(alarm);
            }
        }
        return alarms;
    }

    public GrandTotalAlarmSeverity getGrandTotalOverLimit(String billingPeriod) {
        List<BillingRecord> records = billingRecordRepository.findByBillingPeriod(billingPeriod).stream().map(billingMapper::mapToDomain).toList();
        double grandTotal = 0;
        for (BillingRecord r : records) {
            grandTotal += r.totalCharge();
        }

        if (grandTotal > 7500 && grandTotal < 10000) {
            return GrandTotalAlarmSeverity.LOW;
        }

        return GrandTotalAlarmSeverity.HIGH;
    }
}
