/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository.cloudconnection;

import com.azeem.blueprint.entity.CloudConnectionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloudConnectionRepository extends JpaRepository<CloudConnectionEntity, UUID> {

  List<UUID> findDistinctOwnerUserIdsByStatus(String status);

  List<CloudConnectionEntity> findByOwnerUserId(UUID ownerUserId);

  List<CloudConnectionEntity> findByOwnerUserIdAndStatus(UUID ownerUserId, String status);

  Optional<CloudConnectionEntity> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

  List<CloudConnectionEntity> findByStatus(String status);
}
