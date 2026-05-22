/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.client;

import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.notification.AlarmNotificationPayload;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the external notification microservice.
 *
 * <p>Delivers newly detected alarms as JSON to the notification service's {@code POST /notify}
 * endpoint. Failures are logged but never propagated — alarm persistence is the primary concern,
 * notification delivery is best-effort.
 */
@Component
public class NotificationClient {

  private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

  private final RestClient restClient;

  public NotificationClient(
      @Value("${notification.service.url:http://localhost:3000}") String serviceUrl) {
    this.restClient = RestClient.builder().baseUrl(serviceUrl).build();
  }

  public void sendAlarmNotifications(List<Alarm> alarms) {
    if (alarms.isEmpty()) return;

    List<AlarmNotificationPayload> payloads = alarms.stream().map(this::toPayload).toList();

    restClient
        .post()
        .uri("/notify")
        .contentType(MediaType.APPLICATION_JSON)
        .body(payloads)
        .retrieve()
        .toBodilessEntity();

    log.info("Dispatched {} alarm notification(s) to notification service.", payloads.size());
  }

  private AlarmNotificationPayload toPayload(Alarm alarm) {
    return new AlarmNotificationPayload(
        alarm.datasetId(),
        alarm.billingPeriod(),
        alarm.alarmScope().name(),
        alarm.alarmSeverity().name(),
        alarm.alarmType(),
        alarm.explanation(),
        alarm.employeeId(),
        alarm.phoneNumber(),
        alarm.department() != null ? alarm.department().name() : null,
        alarm.timestamp());
  }
}
