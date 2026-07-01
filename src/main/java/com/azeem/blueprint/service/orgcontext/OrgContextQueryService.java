/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.orgcontext;

import com.azeem.blueprint.config.OrgContextProps;
import com.azeem.blueprint.exception.core.ContextDocNotFoundException;
import com.azeem.blueprint.exception.core.OrgContextDocumentIngestionException;
import com.azeem.blueprint.exception.web.MaximumOrgContextFilesExceededException;
import com.azeem.blueprint.mapper.OrgContextDocumentMapper;
import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import com.azeem.blueprint.repository.OrgContextDocumentRepository;
import com.azeem.blueprint.service.appuser.AppUserService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Enables the retrieval and deletion of OrgContext markdown files. A facade for S3, Postgres
 * relational database, and PGVector database, maintaining records and integrity across the board
 */
@Service
public class OrgContextQueryService {
  Logger log = LoggerFactory.getLogger(OrgContextQueryService.class);
  private final OrgContextS3Service orgContextS3Service;
  private final OrgContextVectorDatabaseService orgContextVectorDatabaseService;
  private final OrgContextProps props;
  private final OrgContextDocumentRepository orgContextDocumentRepository;
  private final OrgContextDocumentMapper orgContextDocumentMapper;
  private final AppUserService appUserService;

  public OrgContextQueryService(
      OrgContextVectorDatabaseService orgContextVectorDatabaseService,
      OrgContextDocumentRepository orgContextDocumentRepository,
      OrgContextDocumentMapper orgContextDocumentMapper,
      OrgContextS3Service orgContextS3Service,
      OrgContextProps props,
      AppUserService appUserService) {
    this.orgContextS3Service = orgContextS3Service;
    this.orgContextVectorDatabaseService = orgContextVectorDatabaseService;
    this.props = props;
    this.orgContextDocumentRepository = orgContextDocumentRepository;
    this.orgContextDocumentMapper = orgContextDocumentMapper;
    this.appUserService = appUserService;
  }

  @Transactional
  public OrgContextDocument ingestDocuments(UUID ownerUserId, MultipartFile multipartFile) {
    appUserService.findOrCreateGuest(ownerUserId);

    long currentFileCount = orgContextDocumentRepository.countByOwnerUserId(ownerUserId);
    if (currentFileCount >= props.getMaxFiles()) {
      throw new MaximumOrgContextFilesExceededException(
          "User " + ownerUserId + " has reached the maximum number of organization context files.");
    }

    UUID id = UUID.randomUUID();
    String s3Key = orgContextS3Service.buildS3KeyForFile(ownerUserId, id, multipartFile);
    OrgContextDocument contextDoc =
        new OrgContextDocument(
            id, ownerUserId, multipartFile.getOriginalFilename(), s3Key, Instant.now());

    orgContextS3Service.uploadUserFile(contextDoc, multipartFile);
    orgContextVectorDatabaseService.ingestDocument(
        contextDoc, readMultipartFileContent(multipartFile));

    log.debug("Successfully ingested documents for for user {}", ownerUserId);

    return orgContextDocumentMapper.mapToDomain(
        orgContextDocumentRepository.save(orgContextDocumentMapper.mapToEntity(contextDoc)));
  }

  public List<OrgContextDocument> getDocuments(UUID ownerId) {
    return orgContextDocumentRepository.findByOwnerUserIdOrderByUploadedAtDesc(ownerId).stream()
        .map(orgContextDocumentMapper::mapToDomain)
        .toList();
  }

  public OrgContextDocument getDocument(UUID ownerId, UUID docId) {
    return orgContextDocumentRepository
        .findByIdAndOwnerUserId(docId, ownerId)
        .map(orgContextDocumentMapper::mapToDomain)
        .orElseThrow(
            () ->
                new ContextDocNotFoundException(
                    "Organization context document not found for owner "
                        + ownerId
                        + " and document "
                        + docId));
  }

  @Transactional
  public void deleteDocument(UUID ownerId, UUID docId) {
    OrgContextDocument orgContextDocument = getDocument(ownerId, docId);
    orgContextS3Service.deleteFile(orgContextDocument);
    orgContextDocumentRepository.deleteByIdAndOwnerUserId(docId, ownerId);
    orgContextVectorDatabaseService.deleteDocument(orgContextDocument);
    log.debug("Successfully purged all traces of document {}", docId);
  }

  @Transactional
  public void deleteAllDocumentsForUser(UUID ownerId) {
    List<OrgContextDocument> documents = getDocuments(ownerId);
    for (OrgContextDocument document : documents) {
      orgContextS3Service.deleteFile(document);
    }
    orgContextVectorDatabaseService.deleteAllDocumentsForUser(documents);
    orgContextDocumentRepository.deleteByOwnerUserId(ownerId);
    log.debug("Successfully purged all document for user {}", ownerId);
  }

  private String readMultipartFileContent(MultipartFile multipartFile) {
    try {
      return new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new OrgContextDocumentIngestionException(
          "Failed to read organization context file.", e);
    }
  }
}
