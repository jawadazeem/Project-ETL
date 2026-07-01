/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import com.azeem.blueprint.service.orgcontext.OrgContextQueryService;
import com.azeem.blueprint.validation.ValidMdFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/org-contexts")
@Tag(name = "OrgContext", description = "Organization context file upload and management")
public class OrgContextController {
  private static final Logger log = LoggerFactory.getLogger(OrgContextController.class);

  private final OrgContextQueryService orgContextQueryService;

  public OrgContextController(OrgContextQueryService orgContextQueryService) {
    this.orgContextQueryService = orgContextQueryService;
  }

  @Operation(summary = "Upload a markdown organization context file")
  @PostMapping
  public ResponseEntity<OrgContextDocument> createOrgContextDocument(
      @RequestHeader("X-User-Id") UUID userId,
      @ValidMdFile @RequestParam("file") MultipartFile file) {
    log.info("POST /org-contexts called by user: {}", userId);
    OrgContextDocument document = orgContextQueryService.ingestDocuments(userId, file);
    return ResponseEntity.ok(document);
  }

  @Operation(summary = "List all organization context files for a user")
  @GetMapping
  public ResponseEntity<List<OrgContextDocument>> listOrgContextDocuments(
      @RequestHeader("X-User-Id") UUID userId) {
    log.info("GET /org-contexts called by user: {}", userId);
    return ResponseEntity.ok(orgContextQueryService.getDocuments(userId));
  }

  @Operation(summary = "Get an organization context file by ID")
  @GetMapping("/{documentId}")
  public ResponseEntity<OrgContextDocument> getOrgContextDocument(
      @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID documentId) {
    log.info("GET /org-contexts/{} called by user: {}", documentId, userId);
    return ResponseEntity.ok(orgContextQueryService.getDocument(userId, documentId));
  }

  @Operation(summary = "Delete an organization context file")
  @DeleteMapping("/{documentId}")
  public ResponseEntity<Void> deleteOrgContextDocument(
      @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID documentId) {
    log.info("DELETE /org-contexts/{} called by user: {}", documentId, userId);
    orgContextQueryService.deleteDocument(userId, documentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Delete all organization context files for a user")
  @DeleteMapping
  public ResponseEntity<Void> deleteAllOrgContextDocuments(
      @RequestHeader("X-User-Id") UUID userId) {
    log.info("DELETE /org-contexts called by user: {}", userId);
    orgContextQueryService.deleteAllDocumentsForUser(userId);
    return ResponseEntity.noContent().build();
  }
}
