package com.azeem.blueprint.model.prediction;

public class DataPoint {
  private String period;
  private double charge;

  public DataPoint() {}

  public DataPoint(String period, double charge) {
    this.period = period;
    this.charge = charge;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public double getCharge() {
    return charge;
  }

  public void setCharge(double charge) {
    this.charge = charge;
  }
}
