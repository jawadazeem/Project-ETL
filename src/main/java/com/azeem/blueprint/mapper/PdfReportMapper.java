/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.mapper;

import com.azeem.blueprint.entity.PdfReportEntity;
import com.azeem.blueprint.model.report.PdfReport;
import org.springframework.stereotype.Component;

@Component
public class PdfReportMapper {

  public PdfReport mapToDomain(PdfReportEntity entity) {
    return new PdfReport(
        entity.getId(),
        entity.getDataset().getId(),
        entity.getUser().getId(),
        entity.getBillingPeriod(),
        entity.getS3ObjectKey(),
        entity.getFileSizeBytes(),
        entity.getGeneratedAt());
  }
}
