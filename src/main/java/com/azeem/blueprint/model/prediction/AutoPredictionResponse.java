package com.azeem.blueprint.model.prediction;

import java.util.List;

public class AutoPredictionResponse {
  private List<DataPoint> historical;
  private List<DataPoint> forecasts;

  public AutoPredictionResponse() {}

  public AutoPredictionResponse(List<DataPoint> historical, List<DataPoint> forecasts) {
    this.historical = historical;
    this.forecasts = forecasts;
  }

  public List<DataPoint> getHistorical() {
    return historical;
  }

  public void setHistorical(List<DataPoint> historical) {
    this.historical = historical;
  }

  public List<DataPoint> getForecasts() {
    return forecasts;
  }

  public void setForecasts(List<DataPoint> forecasts) {
    this.forecasts = forecasts;
  }
}
