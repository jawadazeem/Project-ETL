package com.azeem.billing.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an Alarm.
 * <p>An Alarm object either be for a department or an individual</p>
 */

public record Alarm(UUID id,
                    @Enumerated(EnumType.STRING)
                    AlarmScope alarmScope,
                    String billingPeriod,
                    String alarmType,
                    @Enumerated(EnumType.STRING)
                    AlarmSeverity alarmSeverity,
                    String explanation,
                    Instant timestamp,
                    String employeeId, // nullable
                    String phoneNumber, // nullable
                    @Enumerated(EnumType.STRING)
                    Department department // nullable
    ) {
}
