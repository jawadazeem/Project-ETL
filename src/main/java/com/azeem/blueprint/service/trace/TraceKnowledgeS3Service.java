/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.config.TraceKnowledgeS3Config;
import com.azeem.blueprint.exception.core.TraceKnowledgeNotFoundException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

  /** Aggregates multiple Markdown playbooks from S3 into a single continuous stream. */
  public InputStream getTraceKnowledgeDataStream() {
    log.info(
        "Fetching Trace's playbooks from bucket: {} | Keys: {}",
        props.getBucketName(),
        props.getKeys());

    List<InputStream> inputStreams =
        props.getKeys().stream()
            .map(
                key -> {
                  S3Resource resource = s3Template.download(props.getBucketName(), key);
                  if (!resource.exists()) {
                    throw new TraceKnowledgeNotFoundException(
                        "Trace's data missing in S3: " + key);
                  }
                  try {
                    return resource.getInputStream();
                  } catch (IOException e) {
                    throw new RuntimeException("Failed to open stream for key: " + key, e);
                  }
                })
            .collect(Collectors.toList());

    // SequenceInputStream concatenates multiple input streams into one sequential stream
    return new SequenceInputStream(Collections.enumeration(inputStreams));
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
