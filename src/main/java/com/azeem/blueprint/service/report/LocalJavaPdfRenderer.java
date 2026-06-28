/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import com.azeem.blueprint.exception.core.PdfGenerationException;
import com.azeem.blueprint.model.alarm.Alarm;
import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.model.report.CorporateInfo;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LocalJavaPdfRenderer implements PdfRenderer {

  private static final Font TITLE_FONT =
      new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 41, 59));
  private static final Font HEADING_FONT =
      new Font(Font.HELVETICA, 13, Font.BOLD, new Color(79, 70, 229));
  private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
  private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
  private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
  private static final Color HEADER_BG = new Color(79, 70, 229);
  private static final Color STRIPE_BG = new Color(248, 250, 252);

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a").withZone(ZoneId.systemDefault());

  @Override
  public byte[] render(
      CorporateInfo corporateInfo,
      BillingSummary summary,
      List<Alarm> alarms,
      String billingPeriod) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document doc = new Document(PageSize.LETTER, 40, 40, 40, 40);
      PdfWriter.getInstance(doc, out);
      doc.open();

      addHeader(doc, corporateInfo, billingPeriod);
      addSummarySection(doc, summary);
      addProviderBreakdown(doc, summary.getChargesByProvider());
      addAlarmsSection(doc, alarms);
      addFooter(doc);

      doc.close();
      return out.toByteArray();
    } catch (Exception e) {
      throw new PdfGenerationException("Failed to render PDF report", e);
    }
  }

  private void addHeader(Document doc, CorporateInfo info, String billingPeriod)
      throws DocumentException {
    Paragraph companyName = new Paragraph(info.companyName(), TITLE_FONT);
    doc.add(companyName);

    StringBuilder address = new StringBuilder();
    appendIfPresent(address, info.addressLine1());
    appendIfPresent(address, info.addressLine2());
    StringBuilder cityLine = new StringBuilder();
    if (info.city() != null) cityLine.append(info.city());
    if (info.state() != null) {
      if (!cityLine.isEmpty()) cityLine.append(", ");
      cityLine.append(info.state());
    }
    if (info.zipCode() != null) {
      if (!cityLine.isEmpty()) cityLine.append(" ");
      cityLine.append(info.zipCode());
    }
    if (!cityLine.isEmpty()) address.append(cityLine).append("\n");
    appendIfPresent(address, info.phone());
    appendIfPresent(address, info.email());

    if (!address.isEmpty()) {
      Paragraph addressPara = new Paragraph(address.toString(), BODY_FONT);
      addressPara.setSpacingAfter(4);
      doc.add(addressPara);
    }

    doc.add(new Paragraph(" "));

    Paragraph reportTitle = new Paragraph("Billing Report", HEADING_FONT);
    doc.add(reportTitle);

    Paragraph meta =
        new Paragraph(
            "Billing Period: "
                + billingPeriod
                + "  |  Generated: "
                + DATE_FMT.format(Instant.now()),
            SMALL_FONT);
    meta.setSpacingAfter(16);
    doc.add(meta);

    doc.add(horizontalRule());
  }

  private void addSummarySection(Document doc, BillingSummary summary) throws DocumentException {
    Paragraph heading = new Paragraph("Summary", HEADING_FONT);
    heading.setSpacingBefore(12);
    heading.setSpacingAfter(8);
    doc.add(heading);

    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(60);
    table.setHorizontalAlignment(Element.ALIGN_LEFT);
    table.setWidths(new float[] {1.5f, 1f});

    addMetricRow(table, "Total Records", String.valueOf(summary.getTotalRecords()), false);
    addMetricRow(table, "Total Charges", formatCurrency(summary.getTotalCharges()), true);
    addMetricRow(table, "Average Charge", formatCurrency(summary.getAverageCharge()), false);

    if (summary.getHighestChargeRecord() != null) {
      String highVal =
          formatCurrency(summary.getHighestChargeRecord().totalCharge())
              + " ("
              + summary.getHighestChargeRecord().accountName()
              + ")";
      addMetricRow(table, "Highest Charge", highVal, true);
    }

    table.setSpacingAfter(16);
    doc.add(table);
  }

  private void addProviderBreakdown(Document doc, Map<String, Double> chargesByProvider)
      throws DocumentException {
    if (chargesByProvider == null || chargesByProvider.isEmpty()) return;

    Paragraph heading = new Paragraph("Charges by Cloud Provider", HEADING_FONT);
    heading.setSpacingBefore(8);
    heading.setSpacingAfter(8);
    doc.add(heading);

    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(80);
    table.setWidths(new float[] {2f, 1f});

    addTableHeader(table, "Cloud Provider");
    addTableHeader(table, "Total Charges");

    List<Map.Entry<String, Double>> sorted =
        chargesByProvider.entrySet().stream()
            .sorted(
                Comparator.<Map.Entry<String, Double>, Double>comparing(Map.Entry::getValue)
                    .reversed())
            .toList();

    int row = 0;
    for (Map.Entry<String, Double> entry : sorted) {
      Color bg = (row++ % 2 == 1) ? STRIPE_BG : Color.WHITE;
      addTableCell(table, entry.getKey(), bg, Element.ALIGN_LEFT);
      addTableCell(table, formatCurrency(entry.getValue()), bg, Element.ALIGN_RIGHT);
    }

    table.setSpacingAfter(16);
    doc.add(table);
  }

  private void addAlarmsSection(Document doc, List<Alarm> alarms) throws DocumentException {
    Paragraph heading = new Paragraph("Alarms", HEADING_FONT);
    heading.setSpacingBefore(8);
    heading.setSpacingAfter(8);
    doc.add(heading);

    if (alarms == null || alarms.isEmpty()) {
      doc.add(new Paragraph("No alarms for this billing period.", BODY_FONT));
      return;
    }

    PdfPTable table = new PdfPTable(4);
    table.setWidthPercentage(100);
    table.setWidths(new float[] {1f, 1.5f, 0.8f, 2.5f});

    addTableHeader(table, "Scope");
    addTableHeader(table, "Type");
    addTableHeader(table, "Severity");
    addTableHeader(table, "Explanation");

    int row = 0;
    for (Alarm alarm : alarms) {
      Color bg = (row++ % 2 == 1) ? STRIPE_BG : Color.WHITE;
      addTableCell(table, alarm.alarmScope().name(), bg, Element.ALIGN_LEFT);
      addTableCell(table, alarm.alarmType(), bg, Element.ALIGN_LEFT);
      addTableCell(table, alarm.alarmSeverity().name(), bg, Element.ALIGN_CENTER);
      addTableCell(table, alarm.explanation(), bg, Element.ALIGN_LEFT);
    }

    table.setSpacingAfter(16);
    doc.add(table);
  }

  private void addFooter(Document doc) throws DocumentException {
    doc.add(horizontalRule());
    Paragraph footer =
        new Paragraph(
            "Generated by Blueprint Cloud FinOps Platform  |  " + DATE_FMT.format(Instant.now()),
            SMALL_FONT);
    footer.setAlignment(Element.ALIGN_CENTER);
    footer.setSpacingBefore(8);
    doc.add(footer);
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private Paragraph horizontalRule() {
    Paragraph hr = new Paragraph(" ");
    hr.setSpacingBefore(4);
    hr.setSpacingAfter(4);
    return hr;
  }

  private void addMetricRow(PdfPTable table, String label, String value, boolean striped) {
    Color bg = striped ? STRIPE_BG : Color.WHITE;

    PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
    labelCell.setBorder(Rectangle.NO_BORDER);
    labelCell.setBackgroundColor(bg);
    labelCell.setPadding(6);
    table.addCell(labelCell);

    PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY_FONT));
    valueCell.setBorder(Rectangle.NO_BORDER);
    valueCell.setBackgroundColor(bg);
    valueCell.setPadding(6);
    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(valueCell);
  }

  private void addTableHeader(PdfPTable table, String text) {
    PdfPCell cell =
        new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
    cell.setBackgroundColor(HEADER_BG);
    cell.setPadding(6);
    cell.setBorderWidth(0);
    table.addCell(cell);
  }

  private void addTableCell(PdfPTable table, String text, Color bg, int alignment) {
    PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", BODY_FONT));
    cell.setBackgroundColor(bg);
    cell.setPadding(5);
    cell.setBorderWidth(0.5f);
    cell.setBorderColor(new Color(226, 232, 240));
    cell.setHorizontalAlignment(alignment);
    table.addCell(cell);
  }

  private String formatCurrency(double amount) {
    return String.format("$%,.2f", amount);
  }

  private void appendIfPresent(StringBuilder sb, String value) {
    if (value != null && !value.isBlank()) {
      sb.append(value).append("\n");
    }
  }
}
