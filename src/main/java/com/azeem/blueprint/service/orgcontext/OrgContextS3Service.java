/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.orgcontext;

import com.azeem.blueprint.config.OrgContextProps;
import com.azeem.blueprint.exception.core.ContextDocNotFoundException;
import com.azeem.blueprint.exception.core.OrgContextNotFoundException;
import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Wrapper over AWS SDK for manipulating OrgContext data saved in S3. It is very similar to <code>
 * BillingS3Service</code> This class also caps the amount of data that can be ingested by a given
 * user.
 */
@Component
public class OrgContextS3Service {
  private static final Logger log = LoggerFactory.getLogger(OrgContextS3Service.class);
  private final S3Template s3Template;
  private final S3Operations s3Operations;
  private final OrgContextProps props;

  public OrgContextS3Service(
      S3Template s3Template, S3Operations s3Operations, OrgContextProps props) {
    this.s3Template = s3Template;
    this.s3Operations = s3Operations;
    this.props = props;
  }

  public InputStream getOrgContextDataStream(String key) {
    String bucketName = props.getBucketName();
    log.info("Fetching OrgContext data from S3 bucket: {} with key: {}", bucketName, key);

    S3Resource resource = s3Template.download(props.getBucketName(), key);

    if (!resource.exists()) {
      throw new OrgContextNotFoundException("OrgContext data not found in S3: " + key);
    }

    try {
      return resource.getInputStream();
    } catch (IOException e) {
      log.error("Failed to open InputStream for S3 object: {}", key, e);
      throw new RuntimeException("Error accessing S3 stream", e);
    }
  }

  public void uploadUserFile(OrgContextDocument orgContextDocument, MultipartFile file) {
    String bucketName = props.getBucketName();
    try {
      String key =
          buildS3KeyForFile(orgContextDocument.ownerUserId(), orgContextDocument.id(), file);

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

  public void deleteFile(OrgContextDocument orgContextDoc) {
    String bucketName = props.getBucketName();
    String key = orgContextDoc.s3ObjectKey();

    if (!s3Operations.objectExists(bucketName, key)) {
      throw new ContextDocNotFoundException(
          "Failed to delete file in bucket "
              + bucketName
              + " because it does not exist at key: "
              + key);
    }

    s3Operations.deleteObject(bucketName, key);
  }

  protected String buildS3KeyForFile(
      UUID ownerUserId, UUID orgContextDocumentId, MultipartFile file) {
    return "%s/%s/%s".formatted(ownerUserId, orgContextDocumentId, file.getOriginalFilename());
  }
}
