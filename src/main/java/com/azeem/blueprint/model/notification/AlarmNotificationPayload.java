/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.model.notification;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload sent to the external notification microservice when new alarms are persisted.
 *
 * <p>This is the contract between Blueprint and the notification service. Any field changes here
 * must be mirrored in the notification service's TypeScript interface.
 */
public record AlarmNotificationPayload(
    UUID datasetId,
    String billingPeriod,
    String alarmScope,
    String alarmSeverity,
    String alarmType,
    String explanation,
    @Nullable String employeeId,
    @Nullable String phoneNumber,
    @Nullable String department,
    Instant timestamp) {}
