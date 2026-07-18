/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository.preference;

import com.azeem.blueprint.entity.preference.AlarmThresholdPreferenceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmPreferenceRepository
    extends JpaRepository<AlarmThresholdPreferenceEntity, UUID> {
  Optional<AlarmThresholdPreferenceEntity> findByOwnerUserId(UUID ownerUserId);

  void deleteByOwnerUserId(UUID ownerUserId);
}
