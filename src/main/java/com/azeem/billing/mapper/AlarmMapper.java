package com.azeem.billing.mapper;

import com.azeem.billing.entity.AlarmEntity;
import com.azeem.billing.model.Alarm;
import java.util.Optional;

/**
 * Mapper class to convert between Alarm domain model and AlarmEntity database entity.
 */

public class AlarmMapper {

    public AlarmEntity mapToEntity(Alarm alarm) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setAlarmScope(alarm.alarmScope());
        alarmEntity.setAlarmSeverity(alarm.alarmSeverity());
        alarmEntity.setAlarmType(alarm.alarmType());
        alarmEntity.setBillingPeriod(alarm.billingPeriod());
        alarmEntity.setEmployeeId(alarm.employeeId());
        alarmEntity.setExplanation(alarm.explanation());
        alarmEntity.setPhoneNumber(alarm.phoneNumber());
        alarmEntity.setDepartment(alarm.department());
        alarmEntity.setTimestamp(alarm.timestamp());
        return alarmEntity;
    }

    public Alarm mapToDomain(AlarmEntity alarmEntity) {
        return new Alarm(
                alarmEntity.getId(),
                alarmEntity.getAlarmScope(),
                alarmEntity.getBillingPeriod(),
                alarmEntity.getAlarmType(),
                alarmEntity.getAlarmSeverity(),
                alarmEntity.getExplanation(),
                alarmEntity.getTimestamp(),
                alarmEntity.getEmployeeId(),
                alarmEntity.getPhoneNumber(),
                alarmEntity.getDepartment()
        );
    }
}
