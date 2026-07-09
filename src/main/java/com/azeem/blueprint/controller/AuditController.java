package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.audit.AuditResponse;
import com.azeem.blueprint.service.audit.AuditService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

  private final AuditService auditService;

  public AuditController(AuditService auditService) {
    this.auditService = auditService;
  }

  @PostMapping("/{datasetId}")
  public ResponseEntity<AuditResponse> runAudit(
      @PathVariable UUID datasetId, @RequestParam String billingPeriod) {
    AuditResponse response = auditService.runAudit(datasetId, billingPeriod);
    return ResponseEntity.ok(response);
  }
}