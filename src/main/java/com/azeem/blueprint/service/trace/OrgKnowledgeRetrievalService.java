/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.trace;

import com.azeem.blueprint.service.orgcontext.OrgContextVectorDatabaseService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
  private final VectorStore vectorStore;

  public OrgKnowledgeRetrievalService(
      VectorStore vectorStore, OrgContextVectorDatabaseService orgContextVectorDatabaseService) {
    this.vectorStore = vectorStore;
  }

  /**
   * Filters by the owner user id, upholding tenant scoped lookup.
   *
   * @return List of tenant scoped user uploaded documents.
   */
  public List<Document> retrieveFilteredDocuments(UUID ownerUserId, String query) {
    FilterExpressionBuilder b = new FilterExpressionBuilder();
    SearchRequest request =
        SearchRequest.builder()
            .query(query == null || query.isBlank() ? "cloud billing context" : query)
            .topK(5)
            .filterExpression(b.eq("ownerUserId", ownerUserId.toString()).build())
            .build();

    return this.vectorStore.similaritySearch(request);
  }
}
