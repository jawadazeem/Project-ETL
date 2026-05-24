/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.model.report.CorporateInfo;
import java.util.List;

/**
 * Strategy interface for rendering billing reports to PDF.
 *
 * <p>Implementations may render locally (e.g. OpenPDF) or delegate to a remote service (e.g. AWS
 * Lambda).
 */
public interface PdfRenderer {
  byte[] render(
      CorporateInfo corporateInfo,
      BillingSummary summary,
      List<Alarm> alarms,
      String billingPeriod);
}
