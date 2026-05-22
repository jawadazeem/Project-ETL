/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.listener;

import com.azeem.blueprint.exception.infra.S3SqsPipelineIngestionException;
import com.azeem.blueprint.model.billing.IngestionResult;
import com.azeem.blueprint.repository.DatasetRepository;
import com.azeem.blueprint.service.alarm.AlarmService;
import com.azeem.blueprint.service.billing.BillingIngestionService;
import com.azeem.blueprint.service.billing.BillingS3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BillingEventListener {
  private static final Logger log = LoggerFactory.getLogger(BillingEventListener.class);

  private final BillingS3Service s3Service;
  private final BillingIngestionService ingestionService;
  private final DatasetRepository datasetRepository;
  private final ObjectMapper objectMapper;

  public BillingEventListener(
      BillingS3Service s3Service,
      BillingIngestionService ingestionService,
      AlarmService alarmService,
      DatasetRepository datasetRepository,
      ObjectMapper objectMapper) {
    this.s3Service = s3Service;
    this.ingestionService = ingestionService;
    this.datasetRepository = datasetRepository;
    this.objectMapper = objectMapper;
  }

  @SqsListener("billing-event-queue")
  public void onS3Event(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);

      JsonNode records = root.path("Records");

      if (!records.isArray() || records.isEmpty()) {
        return;
      }

      JsonNode s3Node = records.get(0).path("s3");

      String bucket = s3Node.path("bucket").path("name").asText();
      String rawKey = s3Node.path("object").path("key").asText();

      if (bucket.isEmpty() || rawKey.isEmpty()) {
        log.warn("Received SQS message but could not find S3 metadata. Message: {}", message);
        return;
      }

      // Decodes AWS S3 URL encoding (converts '+' or '%20' back into clean spaces)
      String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);

      // Skips error logs or shallow artifacts from breaking structural string splitting
      String[] keyParts = key.split("/");
      if (keyParts.length < 3 || key.startsWith("error-logs/")) {
        log.debug("Ignoring non-ingestible system log file output: {}", key);
        return;
      }

      UUID datasetId;
      try {
        datasetId = UUID.fromString(keyParts[1]);
      } catch (IllegalArgumentException e) {
        log.warn(
            "Skipping processing. Path section does not contain a valid Dataset UUID: {}",
            keyParts[1]);
        return;
      }

      log.info("Triggering Ingestion for S3 Object: s3://{}/{}", bucket, key);

      try (InputStream s3Stream = s3Service.getBillingDataStream(bucket, key)) {
        IngestionResult result = ingestionService.ingestData(datasetId, s3Stream);

        if (result.failureCount() > 0) {
          s3Service.uploadErrorLog(bucket, result.billingPeriod(), result.errorLog());
        }

        log.info("Event-driven ingestion successful for {}", key);
        markDatasetReady(datasetId);
      }

    } catch (Exception e) {
      log.error("Failed to process S3 event from SQS", e);
      throw new S3SqsPipelineIngestionException(
          "Re-throwing exception to trigger SQS message redelivery", e);
    }
  }

  private void markDatasetReady(UUID datasetId) {
    try {
      datasetRepository
          .findById(datasetId)
          .ifPresent(
              dataset -> {
                dataset.setStatus("READY");
                datasetRepository.save(dataset);
                log.info("Dataset {} status updated to READY", datasetId);
              });
    } catch (Exception e) {
      log.warn("Failed to update dataset status to READY for {}: {}", datasetId, e.getMessage());
    }
  }
}
