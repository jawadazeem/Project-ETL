/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.service.dataset.DatasetService;
import com.azeem.blueprint.validation.ValidCsvFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/datasets")
@Tag(name = "Datasets", description = "Dataset upload and management")
public class DatasetController {
  private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

  private final DatasetService datasetService;

  public DatasetController(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  @Operation(summary = "Upload a CSV file to create a new dataset")
  @PostMapping
  public ResponseEntity<Dataset> createDataset(
      @RequestHeader("X-User-Id") String userId,
      @ValidCsvFile @RequestParam("file") MultipartFile file) {
    log.info("POST /datasets called by user: {}", userId);
    Dataset dataset = datasetService.initializeAndUploadDataset(userId, file);
    return ResponseEntity.ok(dataset);
  }

  @Operation(summary = "List all datasets for a user")
  @GetMapping
  public ResponseEntity<List<Dataset>> listDatasets(@RequestHeader("X-User-Id") UUID userId) {
    log.info("GET /datasets called by user: {}", userId);
    return ResponseEntity.ok(datasetService.listDatasets(userId));
  }

  @Operation(summary = "Get a dataset by ID")
  @GetMapping("/{datasetId}")
  public ResponseEntity<Dataset> getDataset(@PathVariable UUID datasetId) {
    log.info("GET /datasets/{} called.", datasetId);
    return ResponseEntity.ok(datasetService.getDataset(datasetId));
  }
}
