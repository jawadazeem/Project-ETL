/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.billing;

import com.azeem.blueprint.config.S3Config;
import com.azeem.blueprint.exception.core.BillingDataNotFoundException;
import com.azeem.blueprint.model.dataset.Dataset;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BillingS3Service {
  private static final Logger log = LoggerFactory.getLogger(BillingS3Service.class);
  private final S3Template s3Template;
  private final S3Operations s3Operations;
  private final S3Config s3Config;

  public BillingS3Service(S3Template s3Template, S3Operations s3Operations, S3Config s3Config) {
    this.s3Template = s3Template;
    this.s3Operations = s3Operations;
    this.s3Config = s3Config;
  }

  public InputStream getBillingDataStream(String bucketName, String key) {
    log.info("Fetching billing data from S3 bucket: {} with key: {}", bucketName, key);

    S3Resource resource = s3Template.download(bucketName, key);

    if (!resource.exists()) {
      throw new BillingDataNotFoundException("Billing file not found in S3: " + key);
    }

    try {
      return resource.getInputStream();
    } catch (IOException e) {
      log.error("Failed to open InputStream for S3 object: {}", key, e);
      throw new RuntimeException("Error accessing S3 stream", e);
    }
  }

  /**
   * @deprecated This method is deprecated and scheduled for removal. Please transition to using
   *     direct integration with cloud service provider APIs or direct parsing from cloud storage
   *     for billing data retrieval.
   */
  @Deprecated
  public void uploadUserFile(String bucketName, Dataset dataset, MultipartFile file) {
    try {
      String key = buildS3KeyForFile(dataset, file);

      if (s3Operations.objectExists(bucketName, key)) {
        log.warn(
            "File {} already exists in bucket {}, " + "the file cannot be uploaded.",
            key,
            bucketName);

        return;
      }

      log.info("Uploading user file {} to bucket {}", key, bucketName);

      // Streaming directly from the Multipart request to S3
      s3Template.upload(bucketName, key, file.getInputStream());

    } catch (IOException e) {
      log.error("Failed to stream upload to S3", e);
      throw new RuntimeException("S3 Upload Failed");
    }
  }

  /**
   * @return The full directory (key) a dataset resides in, including the bucket name
   */
  public String getDatasetDir(UUID userId, Dataset dataset) {
    return String.format("%s/%s/%s", s3Config.billingBucketName(), userId.toString(), dataset.id());
  }

  private String buildS3KeyForFile(Dataset dataset, MultipartFile file) {
    return "%s/%s/%s".formatted(dataset.ownerUserId(), dataset.id(), file.getOriginalFilename());
  }

  public void uploadErrorLog(String bucketName, String billingPeriod, String errorLogContent) {
    String errorKey = String.format("error-logs/%s-errors.log", billingPeriod);

    try {
      InputStream inputStream =
          new ByteArrayInputStream(errorLogContent.getBytes(StandardCharsets.UTF_8));
      s3Template.upload(bucketName, errorKey, inputStream);
      log.info("Successfully uploaded error log for billing period: {}", billingPeriod);
    } catch (Exception e) {
      log.error("Failed to upload error log to S3 for billing period: {}", billingPeriod, e);
      throw new RuntimeException("Error uploading error log to S3", e);
    }
  }
}
