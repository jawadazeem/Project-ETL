/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.exception.web.QueryLimitExceededException;
import com.azeem.blueprint.model.martin.MartinRequest;
import com.azeem.blueprint.model.martin.MartinResponse;
import com.azeem.blueprint.service.martin.MartinService;
import com.azeem.blueprint.service.martin.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/datasets/{datasetId}")
@Tag(name = "AI Assistant", description = "Natural language billing queries")
public class MartinController {
  private static final Logger log = LoggerFactory.getLogger(MartinController.class);
  private final MartinService martinService;
  private final RateLimiter rateLimiter;

  public MartinController(MartinService martinService, RateLimiter rateLimiter) {
    this.martinService = martinService;
    this.rateLimiter = rateLimiter;
  }

  @Operation(summary = "Ask a natural language question about billing data")
  @PostMapping("/martin")
  public ResponseEntity<MartinResponse> chat(
      @PathVariable String datasetId, @Valid @RequestBody MartinRequest request) {
    if (!rateLimiter.tryAcquire()) {
      throw new QueryLimitExceededException(
          "AI query rate limit exceeded. Please wait a moment before trying again.");
    }
    MartinResponse response =
        martinService.ask(request.getPrompt(), UUID.fromString(datasetId), request.getPeriod());
    return ResponseEntity.ok(response);
  }
}
