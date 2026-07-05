/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import com.azeem.blueprint.service.orgcontext.OrgContextVectorDatabaseService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * Tenant scoped RAG, where the vector database contains organization specific cloud contract and
 * spend context. Unlike <code>OrgContextVectorDatabaseService</code>, this is specifically only to
 * be used by the AI agent.
 *
 * <p>It is responsible for pre-filtering queries at the user level. Filtering queries at both the
 * user and org level is practically the same as they have a one to one mapping.
 */
@Service
public class OrgKnowledgeRetrievalService {
  private static final Logger log = LoggerFactory.getLogger(OrgKnowledgeRetrievalService.class);
  private final OrgContextVectorDatabaseService orgContextVectorDatabaseService;

  public OrgKnowledgeRetrievalService(
      OrgContextVectorDatabaseService orgContextVectorDatabaseService) {
    this.orgContextVectorDatabaseService = orgContextVectorDatabaseService;
  }

  /**
   * Filters using the org context documents' owner id
   *
   */
  public List<Document> retrieveFilteredDocuments(List<OrgContextDocument> docs) {
    UUID uuid = docs.getFirst().ownerUserId();

    for (OrgContextDocument doc : docs) {
      if (!doc.ownerUserId().equals(uuid)) {
        throw new IllegalArgumentException(
            "Fatal Error: Trying to filter using documents that belong to more than one user.");
      }
    }

    FilterExpressionBuilder b = new FilterExpressionBuilder();
    Filter.Expression filterExpression = b.field("ownerUserId").eq(uuid);
  }
}
