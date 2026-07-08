/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.model.trace.TaskType;
import com.azeem.blueprint.model.trace.TraceContext;
import com.azeem.blueprint.service.dataset.DatasetService;
import com.azeem.blueprint.service.trace.sql.SchemaService;
import com.azeem.blueprint.service.trace.task.TaskQueryService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
public class ContextAggregatorService {
  private static final Logger log = LoggerFactory.getLogger(ContextAggregatorService.class);
  private final SchemaService schemaService;
  private final DatasetService datasetService;
  private final OrgKnowledgeRetrievalService orgKnowledgeRetrievalService;
  private final TaskQueryService taskQueryService;

  public ContextAggregatorService(
      SchemaService schemaService,
      DatasetService datasetService,
      OrgKnowledgeRetrievalService orgKnowledgeRetrievalService,
      TaskQueryService taskQueryService) {
    this.schemaService = schemaService;
    this.datasetService = datasetService;
    this.orgKnowledgeRetrievalService = orgKnowledgeRetrievalService;
    this.taskQueryService = taskQueryService;
  }

  public TraceContext getContext(
      String userPromptText, UUID datasetId, String currentBillingPeriod, TaskType taskType) {
    String schema = schemaService.getSchema();
    String playbook = taskQueryService.getPlaybook(taskType);
    UUID ownerUserId = datasetService.getOwnerId(datasetId);

    List<Document> docs =
        orgKnowledgeRetrievalService.retrieveFilteredDocuments(ownerUserId, userPromptText);
    String orgContext = docs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
    log.info(
        "Successfully retrieved context for prompt {} and dataset with ID {}",
        userPromptText,
        datasetId);
    return new TraceContext(
        playbook, datasetId, currentBillingPeriod, ownerUserId, orgContext, schema);
  }
}
