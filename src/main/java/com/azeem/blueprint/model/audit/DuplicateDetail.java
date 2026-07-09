package com.azeem.blueprint.model.audit;

public class DuplicateDetail {
  private String serviceName;
  private String cloudProvider;
  private double totalCharge;
  private String accountName;
  private int count;

  public DuplicateDetail() {}

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getCloudProvider() {
    return cloudProvider;
  }

  public void setCloudProvider(String cloudProvider) {
    this.cloudProvider = cloudProvider;
  }

  public double getTotalCharge() {
    return totalCharge;
  }

  public void setTotalCharge(double totalCharge) {
    this.totalCharge = totalCharge;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}