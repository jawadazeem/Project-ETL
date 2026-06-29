/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.OrgContextDocumentEntity;
import com.azeem.blueprint.model.orgcontext.OrgContextDocument;
import com.azeem.blueprint.repository.AppUserRepository;
import org.springframework.stereotype.Component;

@Component
public class OrgContextDocumentMapper {
  private final AppUserRepository appUserRepository;

  public OrgContextDocumentMapper(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }

  public OrgContextDocumentEntity mapToEntity(OrgContextDocument document) {
    OrgContextDocumentEntity entity = new OrgContextDocumentEntity();
    entity.setId(document.id());
    entity.setOwnerUser(appUserRepository.getReferenceById(document.ownerUserId()));
    entity.setSourceFilename(document.sourceFilename());
    entity.setS3ObjectKey(document.s3ObjectKey());
    entity.setUploadedAt(document.uploadedAt());
    entity.setStatus("UPLOADED");
    return entity;
  }

  public OrgContextDocument mapToDomain(OrgContextDocumentEntity entity) {
    return new OrgContextDocument(
        entity.getId(),
        entity.getOwnerUser().getId(),
        entity.getSourceFilename(),
        entity.getS3ObjectKey(),
        entity.getUploadedAt());
  }
}
