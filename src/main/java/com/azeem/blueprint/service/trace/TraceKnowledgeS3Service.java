/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.config.TraceKnowledgeS3Config;
import com.azeem.blueprint.exception.core.TraceKnowledgeIncompleteException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Not organization specific. This is for general playbook knowledge, defining Trace's behavior for
 * common requests
 */
@Service
public class TraceKnowledgeS3Service {
  private static final Logger log = LoggerFactory.getLogger(TraceKnowledgeS3Service.class);
  private final S3Template s3Template;
  private final TraceKnowledgeS3Config props;

  public TraceKnowledgeS3Service(S3Template s3Template, TraceKnowledgeS3Config props) {
    this.s3Template = s3Template;
    this.props = props;
  }

  /**
   * Aggregates multiple Markdown playbooks from S3 into a Map of their file names and content in
   * the form of a string.
   */
  public Map<String, String> getTraceKnowledgePlaybooks() {
    log.info(
        "Fetching Trace's playbooks from bucket: {} | Keys: {}",
        props.getBucketName(),
        props.getKeys());

    Map<String, String> filenamesAndContent = new HashMap<String, String>();

    for (String key : props.getKeys()) {
      S3Resource resource = s3Template.download(props.getBucketName(), key);
      try {
        filenamesAndContent.put(
            resource.getFilename(), resource.getContentAsString(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new TraceKnowledgeIncompleteException(
            "Fatal Error: Could not load Trace knowledge from S3");
      }
    }

    return filenamesAndContent;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void loadKnowledgeIntoS3() {
    log.info("Starting initial load of Trace knowledge to S3...");
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    for (String fileName : props.getKeys()) {
      // Locates the file in classpath:trace-knowledge/filename.md
      String path = "classpath:" + props.getFolderPath() + "/" + fileName;
      Resource resource = resolver.getResource(path);

      if (resource.exists()) {
        try {
          s3Template.upload(props.getBucketName(), fileName, resource.getInputStream());
          log.info("Successfully uploaded {} to bucket {}", fileName, props.getBucketName());
        } catch (IOException e) {
          log.error("Failed to upload file to S3: {}", fileName, e);
        }
      } else {
        log.warn("Resource not found in classpath: {}", path);
      }
    }
  }
}
