/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.orgcontext;

import com.azeem.blueprint.config.OrgContextProps;
import com.azeem.blueprint.exception.core.ContextDocNotFoundException;
import com.azeem.blueprint.exception.web.MaximumOrgContextFilesExceededException;
import com.azeem.blueprint.mapper.OrgContextDocumentMapper;
import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import com.azeem.blueprint.repository.OrgContextDocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Enables the retrieval and deletion of OrgContext markdown files. A facade for S3, Postgres
 * relational database, and PGVector database, maintaining records and integrity across the board
 */
@Service
public class OrgContextQueryService {
  private final OrgContextS3Service orgContextS3Service;
  private final OrgContextProps props;
  private final OrgContextDocumentRepository orgContextDocumentRepository;
  private final OrgContextDocumentMapper orgContextDocumentMapper;

  public OrgContextQueryService(
      OrgContextS3Service orgContextS3Service,
      OrgContextProps props,
      OrgContextDocumentRepository orgContextDocumentRepository,
      OrgContextDocumentMapper orgContextDocumentMapper) {
    this.orgContextS3Service = orgContextS3Service;
    this.props = props;
    this.orgContextDocumentRepository = orgContextDocumentRepository;
    this.orgContextDocumentMapper = orgContextDocumentMapper;
  }

  // TODO: Ingest and Delete from the PGVector Database too
  @Transactional
  public OrgContextDocument ingestDocuments(UUID ownerUserId, MultipartFile multipartFile) {
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
  }

  @Transactional
  public void deleteAllDocuments(UUID ownerId) {
    List<OrgContextDocument> documents = getDocuments(ownerId);
    for (OrgContextDocument document : documents) {
      orgContextS3Service.deleteFile(document);
    }
    orgContextDocumentRepository.deleteByOwnerUserId(ownerId);
  }
}
