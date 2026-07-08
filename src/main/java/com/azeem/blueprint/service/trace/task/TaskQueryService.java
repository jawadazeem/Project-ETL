/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace.task;

import com.azeem.blueprint.config.TraceKnowledgeS3Config;
import com.azeem.blueprint.exception.core.TraceKnowledgeIncompleteException;
import com.azeem.blueprint.model.trace.TaskType;
import com.azeem.blueprint.service.trace.TraceKnowledgeS3Service;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** TraceService calls uses this class to send Trace the correct playbook for a given task */
@Service
public class TaskQueryService {
  private static final Logger log = LoggerFactory.getLogger(TaskQueryService.class);
  private final Map<TaskType, String> playbooks = new EnumMap<>(TaskType.class);
  private final TraceKnowledgeS3Service traceKnowledgeS3Service;
  private final TraceKnowledgeS3Config props;

  public TaskQueryService(
      TraceKnowledgeS3Service traceKnowledgeS3Service, TraceKnowledgeS3Config props) {
    this.traceKnowledgeS3Service = traceKnowledgeS3Service;
    this.props = props;
  }

  public String getPlaybook(TaskType taskType) {
    TaskType resolvedTaskType = taskType == null ? TaskType.GENERAL : taskType;
    String playbook = playbooks.get(resolvedTaskType);
    if (playbook == null || playbook.isBlank()) {
      throw new TraceKnowledgeIncompleteException(
          "Fatal Error: Trace playbook was not loaded for task " + resolvedTaskType);
    }
    return playbook;
  }

  /**
   * "Predetermined prompts" are prompts hardcoded directly into the application. These serve as the
   * foundational playbooks that Trace uses to execute specific tasks.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void loadTaskPlaybooks() {
    Map<String, String> playbookDocuments = traceKnowledgeS3Service.getTraceKnowledgePlaybooks();

    if (!allTaskPlaybooksLoaded(playbookDocuments)) {
      throw new TraceKnowledgeIncompleteException(
          "Fatal Error: Trace has an incomplete knowledge base for the execution of predetermined prompts.");
    }

    for (Map.Entry<String, String> entry : playbookDocuments.entrySet()) {
      playbooks.put(TaskType.fromFilename(entry.getKey()), entry.getValue());
    }

    log.info("Successfully loaded all task playbooks");
  }

  /** Sanity check to ensure all configured playbooks are loaded successfully */
  private boolean allTaskPlaybooksLoaded(Map<String, String> playbookDocuments) {
    return props.getKeys().stream().distinct().allMatch(playbookDocuments::containsKey)
        && java.util.Arrays.stream(TaskType.values())
            .map(TaskType::getFilename)
            .allMatch(playbookDocuments::containsKey);
  }
}
