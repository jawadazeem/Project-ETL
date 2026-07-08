/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import com.azeem.blueprint.entity.PdfReportEntity;
import com.azeem.blueprint.exception.core.CorporateInfoNotFoundException;
import com.azeem.blueprint.exception.core.PdfReportNotFoundException;
import com.azeem.blueprint.mapper.PdfReportMapper;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.PdfReport;
import com.azeem.blueprint.repository.DatasetRepository;
import com.azeem.blueprint.repository.PdfReportRepository;
import com.azeem.blueprint.service.alarm.AlarmService;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.billing.BillingQueryService;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: Turn this into an AI first report. This should be a summary of the AI's findings and
// re-generate every 10 minutes,
//  Or on Runtime, whatever comes first.
@Service
public class PdfReportService {
  private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

  private final BillingQueryService billingQueryService;
  private final AlarmService alarmService;
  private final CorporateInfoService corporateInfoService;
  private final PdfRenderer pdfRenderer;
  private final PdfStorageService pdfStorageService;
  private final PdfReportRepository pdfReportRepository;
  private final PdfReportMapper pdfReportMapper;
  private final DatasetRepository datasetRepository;
  private final AppUserService appUserService;

  public PdfReportService(
      BillingQueryService billingQueryService,
      AlarmService alarmService,
      CorporateInfoService corporateInfoService,
      PdfRenderer pdfRenderer,
      PdfStorageService pdfStorageService,
      PdfReportRepository pdfReportRepository,
      PdfReportMapper pdfReportMapper,
      DatasetRepository datasetRepository,
      AppUserService appUserService) {
    this.billingQueryService = billingQueryService;
    this.alarmService = alarmService;
    this.corporateInfoService = corporateInfoService;
    this.pdfRenderer = pdfRenderer;
    this.pdfStorageService = pdfStorageService;
    this.pdfReportRepository = pdfReportRepository;
    this.pdfReportMapper = pdfReportMapper;
    this.datasetRepository = datasetRepository;
    this.appUserService = appUserService;
  }

  @Transactional
  public PdfReport generateReport(UUID userId, UUID datasetId, String billingPeriod) {
    log.info(
        "Generating PDF report for user={}, dataset={}, period={}",
        userId,
        datasetId,
        billingPeriod);

    CorporateInfo corpInfo =
        corporateInfoService
            .getCorporateInfo(userId)
            .orElseThrow(() -> new CorporateInfoNotFoundException(userId));

    BillingSummary summary =
        billingQueryService.generateSummaryForPeriodInDataset(datasetId, billingPeriod);

    List<Alarm> alarms = alarmService.getAllAlarmsInDataset(datasetId, billingPeriod);

    byte[] pdfBytes = pdfRenderer.render(corpInfo, summary, alarms, billingPeriod);

    String s3Key = pdfStorageService.store(userId, datasetId, billingPeriod, pdfBytes);

    PdfReportEntity entity = new PdfReportEntity();
    entity.setDataset(datasetRepository.getReferenceById(datasetId));
    entity.setUser(appUserService.getAppUserEntityById(userId));
    entity.setBillingPeriod(billingPeriod);
    entity.setS3ObjectKey(s3Key);
    entity.setFileSizeBytes((long) pdfBytes.length);

    PdfReportEntity saved = pdfReportRepository.save(entity);
    log.info("PDF report saved with id={}, s3Key={}", saved.getId(), s3Key);

    return pdfReportMapper.mapToDomain(saved);
  }

  @Transactional(readOnly = true)
  public InputStream downloadReport(UUID datasetId, UUID reportId) {
    PdfReportEntity entity =
        pdfReportRepository
            .findByIdAndDatasetId(reportId, datasetId)
            .orElseThrow(() -> new PdfReportNotFoundException(reportId));

    return pdfStorageService.retrieve(entity.getS3ObjectKey());
  }
}
