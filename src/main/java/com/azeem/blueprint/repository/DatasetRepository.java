/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import com.azeem.blueprint.entity.DatasetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface DatasetRepository extends JpaRepository<DatasetEntity, UUID> {
  Optional<DatasetEntity> findByIdAndOwnerUserId(UUID datasetId, UUID ownerUserId);

  List<DatasetEntity> findByOwnerUserId(UUID ownerUserId);

  List<DatasetEntity> findByOwnerUserIdOrOwnerUserIsNull(UUID ownerUserId);

  @Modifying
  @Transactional
  int deleteByIdAndOwnerUserId(UUID datasetId, UUID ownerUserId);
}
