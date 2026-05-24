/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.repository;

import com.azeem.blueprint.entity.PdfReportEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PdfReportRepository extends JpaRepository<PdfReportEntity, UUID> {
  Optional<PdfReportEntity> findByIdAndDatasetId(UUID id, UUID datasetId);
}
