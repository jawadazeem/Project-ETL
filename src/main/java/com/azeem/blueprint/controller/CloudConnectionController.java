/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.cloudconnection.*;
import com.azeem.blueprint.service.cloudconnection.CloudConnectionService;
import com.azeem.blueprint.service.cloudconnection.CloudPollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cloud-connections")
@Tag(name = "Cloud Connections", description = "Multi-cloud connection management")
public class CloudConnectionController {
  private static final Logger log = LoggerFactory.getLogger(CloudConnectionController.class);

  private final CloudConnectionService connectionService;
  private final CloudPollService cloudPollService;

  public CloudConnectionController(
      CloudConnectionService connectionService, CloudPollService cloudPollService) {
    this.connectionService = connectionService;
    this.cloudPollService = cloudPollService;
  }

  @Operation(summary = "Create a new cloud connection")
  @PostMapping
  public ResponseEntity<CloudConnection> createConnection(
      @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody CloudConnectionRequest request) {
    log.info("POST /cloud-connections called by user: {}", userId);
    return ResponseEntity.ok(connectionService.createConnection(userId, request));
  }

  @Operation(summary = "List all cloud connections for a user")
  @GetMapping
  public ResponseEntity<List<CloudConnection>> listConnections(
      @RequestHeader("X-User-Id") UUID userId) {
    log.info("GET /cloud-connections called by user: {}", userId);
    return ResponseEntity.ok(connectionService.listConnections(userId));
  }

  @Operation(summary = "Get a cloud connection by ID")
  @GetMapping("/{connectionId}")
  public ResponseEntity<CloudConnection> getConnection(
      @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID connectionId) {
    log.info("GET /cloud-connections/{} called by user: {}", connectionId, userId);
    return ResponseEntity.ok(connectionService.getConnection(connectionId, userId));
  }

  @Operation(summary = "Delete a cloud connection")
  @DeleteMapping("/{connectionId}")
  public ResponseEntity<Void> deleteConnection(
      @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID connectionId) {
    log.info("DELETE /cloud-connections/{} called by user: {}", connectionId, userId);
    connectionService.deleteConnection(connectionId, userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Update connection status (activate/deactivate)")
  @PatchMapping("/{connectionId}/status")
  public ResponseEntity<CloudConnection> updateStatus(
      @RequestHeader("X-User-Id") UUID userId,
      @PathVariable UUID connectionId,
      @RequestBody Map<String, String> body) {
    log.info("PATCH /cloud-connections/{}/status called by user: {}", connectionId, userId);
    CloudConnectionStatus status = CloudConnectionStatus.valueOf(body.get("status").toUpperCase());
    return ResponseEntity.ok(connectionService.updateStatus(connectionId, userId, status));
  }

  @Operation(summary = "Trigger immediate sync for all active connections")
  @PostMapping("/sync")
  public ResponseEntity<Map<String, Object>> syncConnections(
      @RequestHeader("X-User-Id") UUID userId) {
    log.info("POST /cloud-connections/sync called by user: {}", userId);
    int queued = cloudPollService.triggerSync(userId);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("connectionsQueued", queued);
    return ResponseEntity.ok(response);
  }
}
