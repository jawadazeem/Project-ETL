/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.client;

import com.azeem.blueprint.model.alarm.Alarm;
import java.util.List;
import java.util.Map;
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
 * endpoint. Each alarm is sent individually to match the TypeScript service's Zod validation
 * schema. Failures are logged but never propagated — alarm persistence is the primary concern,
 * notification delivery is best-effort.
 */
@Component
public class NotificationClient {

  private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

  private final RestClient restClient;
  private final List<String> recipientEmails;

  public NotificationClient(
      @Value("${notification.service.url:http://localhost:3001}") String serviceUrl,
      @Value("${notification.recipients.email:}") String recipientEmailsCsv) {
    this.restClient = RestClient.builder().baseUrl(serviceUrl).build();
    this.recipientEmails =
        recipientEmailsCsv.isBlank()
            ? List.of()
            : List.of(recipientEmailsCsv.split(",")).stream().map(String::trim).toList();
  }

  public void sendAlarmNotifications(List<Alarm> alarms) {
    if (alarms.isEmpty()) return;

    for (Alarm alarm : alarms) {
      try {
        Map<String, Object> payload = buildPayload(alarm);
        restClient
            .post()
            .uri("/notify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
      } catch (Exception e) {
        log.warn(
            "Failed to dispatch notification for alarm {} — {}",
            alarm.businessKey(),
            e.getMessage());
      }
    }

    log.info("Dispatched {} alarm notification(s) to notification service.", alarms.size());
  }

  /**
   * Fetches recent notifications from the notification microservice.
   *
   * @param limit maximum number of notifications to retrieve
   * @return raw JSON response as a string
   */
  public String fetchNotifications(int limit) {
    return restClient
        .get()
        .uri("/notifications?limit={limit}", limit)
        .retrieve()
        .body(String.class);
  }

  private Map<String, Object> buildPayload(Alarm alarm) {
    return Map.of(
        "alarmId", alarm.businessKey().toString(),
        "datasetId", alarm.datasetId().toString(),
        "billingPeriod", alarm.billingPeriod(),
        "severity", alarm.alarmSeverity().name(),
        "title", alarm.alarmType(),
        "message", alarm.explanation(),
        "recipients", Map.of("email", recipientEmails, "slackWebhooks", List.of()));
  }
}
