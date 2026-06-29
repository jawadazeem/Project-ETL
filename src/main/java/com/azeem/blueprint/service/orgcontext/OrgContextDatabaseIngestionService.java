/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.orgcontext;

import com.azeem.blueprint.config.OrgContextProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** Ingests organization context md files into the vector database */
@Service
public class OrgContextDatabaseIngestionService {
  private static final Logger log =
      LoggerFactory.getLogger(OrgContextDatabaseIngestionService.class);
  private final VectorStore vectorStore;
  private final OrgContextS3Service orgContextS3Service;
  private final OrgContextProps props;

  public OrgContextDatabaseIngestionService(
      VectorStore vectorStore, OrgContextS3Service orgContextS3Service, OrgContextProps props) {
    this.vectorStore = vectorStore;
    this.orgContextS3Service = orgContextS3Service;
    this.props = props;
  }

  public void ingest() {}
}
