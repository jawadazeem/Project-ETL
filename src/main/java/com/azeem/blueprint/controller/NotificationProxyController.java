/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.client.NotificationClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(
    name = "Notifications",
    description = "Notification delivery log from the notification microservice")
public class NotificationProxyController {

  private final NotificationClient notificationClient;

  public NotificationProxyController(NotificationClient notificationClient) {
    this.notificationClient = notificationClient;
  }

  @Operation(summary = "Get recent notification delivery history")
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getNotifications(@RequestParam(defaultValue = "50") int limit) {
    String json = notificationClient.fetchNotifications(Math.min(Math.max(limit, 1), 100));
    return ResponseEntity.ok(json);
  }
}
