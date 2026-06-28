/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.etl.CsvExportService;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.service.alarm.AlarmService;
import com.azeem.blueprint.validation.BillingPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/datasets/{datasetId}")
@Tag(name = "Alarms", description = "Cloud spend alarm detection and queries")
public class AlarmController {
  private static final Logger log = LoggerFactory.getLogger(AlarmController.class);
  private final AlarmService service;
  private final CsvExportService csvExportService;

  public AlarmController(AlarmService service, CsvExportService csvExportService) {
    this.service = service;
    this.csvExportService = csvExportService;
  }

  @Operation(summary = "Get all alarms for a billing period")
  @GetMapping("/alarms/{billingPeriod}")
  public List<Alarm> getAllAlarms(
      @PathVariable UUID datasetId, @BillingPeriod @PathVariable String billingPeriod) {
    log.info("GET /datasets/{}/alarms/{} called.", datasetId, billingPeriod);
    return service.getAllAlarmsInDataset(datasetId, billingPeriod);
  }

  @Operation(summary = "Get provider-scoped alarms")
  @GetMapping("/alarms/{billingPeriod}/provider")
  public List<Alarm> getProviderAlarms(
      @PathVariable UUID datasetId, @BillingPeriod @PathVariable String billingPeriod) {
    log.info("GET /datasets/{}/alarms/{}/provider called.", datasetId, billingPeriod);
    return service.getProviderAlarmsInDataset(datasetId, billingPeriod);
  }

  @Operation(summary = "Get resource-scoped alarms")
  @GetMapping("/alarms/{billingPeriod}/resource")
  public List<Alarm> getResourceAlarms(
      @PathVariable UUID datasetId, @BillingPeriod @PathVariable String billingPeriod) {
    log.info("GET /datasets/{}/alarms/{}/resource called.", datasetId, billingPeriod);
    return service.getResourceAlarmsInDataset(datasetId, billingPeriod);
  }

  @Operation(summary = "Get account-level alarms")
  @GetMapping("/alarms/{billingPeriod}/account")
  public List<Alarm> getAccountAlarm(
      @PathVariable UUID datasetId, @BillingPeriod @PathVariable String billingPeriod) {
    log.info("GET /datasets/{}/alarms/{}/account called.", datasetId, billingPeriod);
    return service.getAccountAlarm(datasetId, billingPeriod);
  }

  @Operation(summary = "Export alarms as CSV")
  @GetMapping(value = "/alarms/{billingPeriod}/export", produces = "text/csv")
  public void exportAlarms(
      @PathVariable UUID datasetId,
      @BillingPeriod @PathVariable String billingPeriod,
      HttpServletResponse response)
      throws IOException {
    log.info("GET /datasets/{}/alarms/{}/export called.", datasetId, billingPeriod);
    response.setContentType("text/csv");
    response.setHeader(
        "Content-Disposition", "attachment; filename=\"alarms-" + billingPeriod + ".csv\"");
    List<Alarm> alarms = service.getAllAlarmsInDataset(datasetId, billingPeriod);
    csvExportService.writeAlarms(alarms, response.getOutputStream());
  }
}
