package com.azeem.blueprint.model.audit;

import java.util.List;

public class AuditFinding {
  private String type;
  private String severity;
  private String description;
  private List<DuplicateDetail> duplicates;

  public AuditFinding() {}

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<DuplicateDetail> getDuplicates() {
    return duplicates;
  }

  public void setDuplicates(List<DuplicateDetail> duplicates) {
    this.duplicates = duplicates;
  }
}