/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PdfStorageService {
  private static final Logger log = LoggerFactory.getLogger(PdfStorageService.class);

  private final S3Template s3Template;
  private final String bucketName;

  public PdfStorageService(
      S3Template s3Template,
      @Value("${spring.cloud.aws.s3.bucket:telecom-billing}") String bucketName) {
    this.s3Template = s3Template;
    this.bucketName = bucketName;
  }

  public String store(UUID userId, UUID datasetId, String billingPeriod, byte[] pdfBytes) {
    String key =
        "%s/%s/reports/%s-report.pdf".formatted(userId, datasetId, billingPeriod);
    log.info("Uploading PDF report to S3: {}", key);
    s3Template.upload(bucketName, key, new ByteArrayInputStream(pdfBytes));
    return key;
  }

  public InputStream retrieve(String s3Key) {
    log.info("Downloading PDF report from S3: {}", s3Key);
    try {
      return s3Template.download(bucketName, s3Key).getInputStream();
    } catch (IOException e) {
      log.error("Failed to download PDF from S3: {}", s3Key, e);
      throw new RuntimeException("Error accessing PDF in S3", e);
    }
  }
}
