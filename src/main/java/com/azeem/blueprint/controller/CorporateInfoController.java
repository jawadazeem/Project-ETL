/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.CorporateInfoRequest;
import com.azeem.blueprint.service.report.CorporateInfoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/corporate-info")
public class CorporateInfoController {
  private static final Logger log = LoggerFactory.getLogger(CorporateInfoController.class);

  private final CorporateInfoService corporateInfoService;

  public CorporateInfoController(CorporateInfoService corporateInfoService) {
    this.corporateInfoService = corporateInfoService;
  }

  @GetMapping
  public ResponseEntity<CorporateInfo> getCorporateInfo(@PathVariable UUID userId) {
    log.info("GET /users/{}/corporate-info", userId);
    return corporateInfoService
        .getCorporateInfo(userId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping
  public ResponseEntity<CorporateInfo> upsertCorporateInfo(
      @PathVariable UUID userId, @Valid @RequestBody CorporateInfoRequest request) {
    log.info("PUT /users/{}/corporate-info", userId);
    CorporateInfo saved = corporateInfoService.upsertCorporateInfo(userId, request);
    return ResponseEntity.ok(saved);
  }
}
