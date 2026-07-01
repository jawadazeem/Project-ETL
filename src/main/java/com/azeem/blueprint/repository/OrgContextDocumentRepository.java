/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import com.azeem.blueprint.entity.OrgContextDocumentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgContextDocumentRepository
    extends JpaRepository<OrgContextDocumentEntity, UUID> {
  List<OrgContextDocumentEntity> findByOwnerUserIdOrderByUploadedAtDesc(UUID ownerUserId);

  Optional<OrgContextDocumentEntity> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

  long countByOwnerUserId(UUID ownerUserId);

  void deleteByIdAndOwnerUserId(UUID id, UUID ownerUserId);

  void deleteByOwnerUserId(UUID ownerUserId);
}
