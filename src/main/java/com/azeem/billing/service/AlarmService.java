package com.azeem.billing.service;

import com.azeem.billing.mapper.AlarmMapper;
import com.azeem.billing.mapper.BillingRecordMapper;
import com.azeem.billing.model.*;
import com.azeem.billing.repository.AlarmRepository;
import com.azeem.billing.repository.BillingRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.azeem.billing.model.AlarmSeverity.*;

@Service
public class AlarmService {

    AlarmRepository alarmRepository;
    BillingRecordRepository billingRecordRepository;
    AlarmMapper alarmMapper = new AlarmMapper();
    BillingRecordMapper billingMapper = new BillingRecordMapper();
    AlarmDetectionService alarmDetectionService;

    public AlarmService(AlarmRepository alarmRepository,
                        BillingRecordRepository billingRecordRepository,
                        AlarmDetectionService alarmDetectionService) {
        this.alarmRepository = alarmRepository;
        this.billingRecordRepository = billingRecordRepository;
        this.alarmDetectionService = alarmDetectionService;
    }

    public void detectAndPersistAlarms(String billingPeriod) {
        List<BillingRecord> records = billingRecordRepository.findByBillingPeriod(billingPeriod).stream().map(billingMapper::mapToDomain).toList();
        List<Alarm> previouslyPersistedAlarms = alarmRepository.findByBillingPeriod(billingPeriod).stream().map(alarmMapper::mapToDomain).toList();
        List<Alarm> detectedAlarms = alarmDetectionService.detectAlarms(records, billingPeriod);
        for (Alarm a : detectedAlarms) {
            if (!(alarmRepository.existsById(a.id()))) {
                alarmRepository.save(alarmMapper.mapToEntity(a));
            }
        }
    }

    public List<Alarm> getAllAlarms(String billingPeriod) {
        return alarmRepository
                .findByBillingPeriod(billingPeriod)
                .stream()
                .map(alarmMapper::mapToDomain)
                .toList();
    }

    public List<Alarm> getDepartmentAlarms(String billingPeriod) {
        return alarmRepository
                .findByBillingPeriodAndAlarmScope(billingPeriod, AlarmScope.DEPARTMENT)
                .stream()
                .map(alarmMapper::mapToDomain)
                .toList();
    }

    public List<Alarm> getIndividualAlarms(String billingPeriod) {
        return alarmRepository
                .findByBillingPeriodAndAlarmScope(billingPeriod, AlarmScope.INDIVIDUAL)
                .stream()
                .map(alarmMapper::mapToDomain)
                .toList();
    }

    public List<Alarm> getAccountAlarm(String billingPeriod) {
        return alarmRepository
                .findByBillingPeriodAndAlarmScope(billingPeriod, AlarmScope.ACCOUNT)
                .stream()
                .map(alarmMapper::mapToDomain)
                .toList();
    }
}
