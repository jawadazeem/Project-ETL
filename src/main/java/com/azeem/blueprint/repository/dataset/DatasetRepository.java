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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DatasetRepository extends JpaRepository<DatasetEntity, UUID> {
  Optional<DatasetEntity> findByIdAndOwnerUserId(UUID datasetId, UUID ownerUserId);

  List<DatasetEntity> findByOwnerUserId(UUID ownerUserId);

  List<DatasetEntity> findByOwnerUserIdOrOwnerUserIsNull(UUID ownerUserId);

  @Query(
      "SELECT d FROM DatasetEntity d"
          + " WHERE (d.ownerUser.id = :userId OR d.ownerUser IS NULL)"
          + " AND d.archived = false")
  List<DatasetEntity> findActiveDatasets(@Param("userId") UUID userId);

  @Query("SELECT d FROM DatasetEntity d WHERE d.ownerUser.id = :userId AND d.archived = true")
  List<DatasetEntity> findArchivedDatasets(@Param("userId") UUID userId);

  @Modifying
  @Transactional
  int deleteByIdAndOwnerUserId(UUID datasetId, UUID ownerUserId);
}
