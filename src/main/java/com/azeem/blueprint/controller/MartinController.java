/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.martin.MartinRequest;
import com.azeem.blueprint.model.martin.MartinResponse;
import com.azeem.blueprint.service.martin.MartinService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/datasets/{datasetId}")
public class MartinController {
  private static final Logger log = LoggerFactory.getLogger(MartinController.class);
  private final MartinService martinService;

  public MartinController(MartinService martinService) {
    this.martinService = martinService;
  }

  @PostMapping("/martin")
  public ResponseEntity<MartinResponse> chat(
      @PathVariable String datasetId, @RequestBody MartinRequest request) {
    MartinResponse response =
        martinService.ask(request.getPrompt(), UUID.fromString(datasetId), request.getPeriod());
    return ResponseEntity.ok(response);
  }
}
