/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.exception.web.QueryLimitExceededException;
import com.azeem.blueprint.model.trace.TraceRequest;
import com.azeem.blueprint.model.trace.TraceResponse;
import com.azeem.blueprint.service.trace.AiExecutionGateway;
import com.azeem.blueprint.service.trace.RateLimiter;
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
public class TraceController {
  private static final Logger log = LoggerFactory.getLogger(TraceController.class);
  private final AiExecutionGateway aiExecutionGateway;
  private final RateLimiter rateLimiter;

  public TraceController(AiExecutionGateway aiExecutionGateway, RateLimiter rateLimiter) {
    this.aiExecutionGateway = aiExecutionGateway;
    this.rateLimiter = rateLimiter;
  }

  @Operation(summary = "Ask a natural language question about billing data")
  @PostMapping("/trace")
  public ResponseEntity<TraceResponse> chat(
      @PathVariable String datasetId, @Valid @RequestBody TraceRequest request) {
    if (!rateLimiter.tryAcquire()) {
      throw new QueryLimitExceededException(
          "AI query rate limit exceeded. Please wait a moment before trying again.");
    }
    TraceResponse response =
        aiExecutionGateway.ask(
            request.getPrompt(), UUID.fromString(datasetId), request.getCurrentPeriod());
    return ResponseEntity.ok(response);
  }
}
