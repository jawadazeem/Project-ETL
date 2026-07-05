/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.orgcontext;

import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** Ingests and deletes organization context md files into the vector database */
@Service
public class OrgContextVectorDatabaseService {
  private static final Logger log = LoggerFactory.getLogger(OrgContextVectorDatabaseService.class);
  private final VectorStore vectorStore;

  public OrgContextVectorDatabaseService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public void ingestDocument(OrgContextDocument doc, String content) {
    Map<String, Object> metadata =
        Map.of(
            "ownerUserId", doc.ownerUserId().toString(), // String-ify for filter compatibility
            "sourceFilename", doc.sourceFilename(),
            "s3ObjectKey", doc.s3ObjectKey(),
            "uploadedAt", doc.uploadedAt().toString());

    Document document = new Document(doc.id().toString(), content, metadata);
    vectorStore.add(List.of(document));
  }

  public void deleteDocument(OrgContextDocument doc) {
    vectorStore.delete(List.of(doc.id().toString()));
  }

  public void deleteAllDocumentsForUser(List<OrgContextDocument> docs) {
    for (OrgContextDocument doc : docs) {
      vectorStore.delete(List.of(doc.id().toString()));
    }
  }
}
