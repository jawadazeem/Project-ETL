/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller.preference;

import com.azeem.blueprint.model.preference.AlarmThresholdPreference;
import com.azeem.blueprint.service.preference.AlarmPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/preferences/alarm-threshold")
@Tag(name = "Alarm Preferences", description = "User-specific alarm threshold preferences")
public class AlarmPreferenceController {
  private static final Logger log = LoggerFactory.getLogger(AlarmPreferenceController.class);

  private final AlarmPreferenceService alarmPreferenceService;

  public AlarmPreferenceController(AlarmPreferenceService alarmPreferenceService) {
    this.alarmPreferenceService = alarmPreferenceService;
  }

  @Operation(summary = "List all alarm threshold preferences")
  @GetMapping
  public ResponseEntity<List<AlarmThresholdPreference>> listPreferences() {
    log.info("GET /preferences/alarm-threshold called.");
    return ResponseEntity.ok(alarmPreferenceService.listPreferences());
  }

  @Operation(summary = "Get alarm threshold preferences for the current user")
  @GetMapping("/me")
  public ResponseEntity<AlarmThresholdPreference> getPreference(
      @RequestHeader("X-User-Id") UUID userId) {
    log.info("GET /preferences/alarm-threshold/me called by user: {}", userId);
    return ResponseEntity.ok(alarmPreferenceService.getPreference(userId));
  }

  @Operation(summary = "Create alarm threshold preferences for the current user")
  @PostMapping("/me")
  public ResponseEntity<AlarmThresholdPreference> createPreference(
      @RequestHeader("X-User-Id") UUID userId,
      @Valid @RequestBody AlarmThresholdPreference preference) {
    log.info("POST /preferences/alarm-threshold/me called by user: {}", userId);
    return ResponseEntity.ok(alarmPreferenceService.savePreference(userId, preference));
  }

  @Operation(summary = "Update alarm threshold preferences for the current user")
  @PutMapping("/me")
  public ResponseEntity<AlarmThresholdPreference> updatePreference(
      @RequestHeader("X-User-Id") UUID userId,
      @Valid @RequestBody AlarmThresholdPreference preference) {
    log.info("PUT /preferences/alarm-threshold/me called by user: {}", userId);
    return ResponseEntity.ok(alarmPreferenceService.savePreference(userId, preference));
  }

  @Operation(summary = "Update alarm threshold preferences and recompute alarms for the current user")
  @PutMapping("/me/recompute")
  public ResponseEntity<AlarmThresholdPreference> updatePreferenceAndRecompute(
      @RequestHeader("X-User-Id") UUID userId,
      @Valid @RequestBody AlarmThresholdPreference preference) {
    log.info("PUT /preferences/alarm-threshold/me/recompute called by user: {}", userId);
    return ResponseEntity.ok(
        alarmPreferenceService.updatePreferenceAndRecompute(userId, preference));
  }

  @Operation(summary = "Delete alarm threshold preferences for the current user")
  @DeleteMapping("/me")
  public ResponseEntity<Void> deletePreference(@RequestHeader("X-User-Id") UUID userId) {
    log.info("DELETE /preferences/alarm-threshold/me called by user: {}", userId);
    alarmPreferenceService.deletePreference(userId);
    return ResponseEntity.noContent().build();
  }
}
