/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.azeem.blueprint.entity.AppUserEntity;
import com.azeem.blueprint.entity.DatasetEntity;
import com.azeem.blueprint.entity.PdfReportEntity;
import com.azeem.blueprint.exception.core.CorporateInfoNotFoundException;
import com.azeem.blueprint.exception.core.PdfReportNotFoundException;
import com.azeem.blueprint.mapper.PdfReportMapper;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.azeem.blueprint.model.report.PdfReport;
import com.azeem.blueprint.repository.PdfReportRepository;
import com.azeem.blueprint.repository.dataset.DatasetRepository;
import com.azeem.blueprint.service.alarm.AlarmService;
import com.azeem.blueprint.service.appuser.AppUserService;
import com.azeem.blueprint.service.billing.BillingQueryService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID REPORT_ID = UUID.randomUUID();
  private static final String PERIOD = "2026-01";

  @Mock private BillingQueryService billingQueryService;
  @Mock private AlarmService alarmService;
  @Mock private CorporateInfoService corporateInfoService;
  @Mock private PdfRenderer pdfRenderer;
  @Mock private PdfStorageService pdfStorageService;
  @Mock private PdfReportRepository pdfReportRepository;
  @Mock private PdfReportMapper pdfReportMapper;
  @Mock private DatasetRepository datasetRepository;
  @Mock private AppUserService appUserService;

  @InjectMocks private PdfReportService pdfReportService;

  @Test
  @DisplayName("Generates report successfully with all data")
  void shouldGenerateReportSuccessfully() {
    CorporateInfo corpInfo = makeCorporateInfo();
    BillingSummary summary = new BillingSummary();
    List<Alarm> alarms = Collections.emptyList();
    byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
    PdfReportEntity savedEntity = new PdfReportEntity();
    PdfReport expected = makePdfReport();

    when(corporateInfoService.getCorporateInfo(USER_ID)).thenReturn(Optional.of(corpInfo));
    when(billingQueryService.generateSummaryForPeriodInDataset(DATASET_ID, PERIOD))
        .thenReturn(summary);
    when(alarmService.getAllAlarmsInDataset(DATASET_ID, PERIOD)).thenReturn(alarms);
    when(pdfRenderer.render(corpInfo, summary, alarms, PERIOD)).thenReturn(pdfBytes);
    when(pdfStorageService.store(USER_ID, DATASET_ID, PERIOD, pdfBytes)).thenReturn("s3/key.pdf");
    when(datasetRepository.getReferenceById(DATASET_ID)).thenReturn(new DatasetEntity());
    when(appUserService.getAppUserEntityById(USER_ID)).thenReturn(new AppUserEntity());
    when(pdfReportRepository.save(any(PdfReportEntity.class))).thenReturn(savedEntity);
    when(pdfReportMapper.mapToDomain(savedEntity)).thenReturn(expected);

    PdfReport result = pdfReportService.generateReport(USER_ID, DATASET_ID, PERIOD);

    assertThat(result).isEqualTo(expected);
    verify(pdfRenderer).render(corpInfo, summary, alarms, PERIOD);
    verify(pdfStorageService).store(USER_ID, DATASET_ID, PERIOD, pdfBytes);
    verify(pdfReportRepository).save(any(PdfReportEntity.class));
  }

  @Test
  @DisplayName("Throws when corporate info is missing")
  void shouldThrowWhenCorporateInfoMissing() {
    when(corporateInfoService.getCorporateInfo(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> pdfReportService.generateReport(USER_ID, DATASET_ID, PERIOD))
        .isInstanceOf(CorporateInfoNotFoundException.class);
  }

  @Test
  @DisplayName("Downloads report successfully")
  void shouldDownloadReportSuccessfully() {
    PdfReportEntity entity = new PdfReportEntity();
    entity.setS3ObjectKey("s3/key.pdf");
    InputStream expected = new ByteArrayInputStream(new byte[0]);

    when(pdfReportRepository.findByIdAndDatasetId(REPORT_ID, DATASET_ID))
        .thenReturn(Optional.of(entity));
    when(pdfStorageService.retrieve("s3/key.pdf")).thenReturn(expected);

    InputStream result = pdfReportService.downloadReport(DATASET_ID, REPORT_ID);

    assertThat(result).isSameAs(expected);
  }

  @Test
  @DisplayName("Throws when report not found for download")
  void shouldThrowWhenReportNotFound() {
    when(pdfReportRepository.findByIdAndDatasetId(REPORT_ID, DATASET_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> pdfReportService.downloadReport(DATASET_ID, REPORT_ID))
        .isInstanceOf(PdfReportNotFoundException.class);
  }

  private CorporateInfo makeCorporateInfo() {
    return new CorporateInfo(
        UUID.randomUUID(),
        USER_ID,
        "Acme Corp",
        "123 Main St",
        null,
        "Springfield",
        "IL",
        "62701",
        "US",
        "555-0100",
        "info@acme.com",
        null,
        Instant.now(),
        Instant.now());
  }

  private PdfReport makePdfReport() {
    return new PdfReport(
        REPORT_ID, DATASET_ID, USER_ID, PERIOD, "s3/key.pdf", 1024L, Instant.now());
  }
}
