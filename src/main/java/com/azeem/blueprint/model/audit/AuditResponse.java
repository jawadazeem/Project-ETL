package com.azeem.blueprint.model.audit;

import java.util.List;

public class AuditResponse {
  private List<AuditFinding> findings;
  private int totalRecordsScanned;

  public AuditResponse() {}

  public List<AuditFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<AuditFinding> findings) {
    this.findings = findings;
  }

  public int getTotalRecordsScanned() {
    return totalRecordsScanned;
  }

  public void setTotalRecordsScanned(int totalRecordsScanned) {
    this.totalRecordsScanned = totalRecordsScanned;
  }
}
