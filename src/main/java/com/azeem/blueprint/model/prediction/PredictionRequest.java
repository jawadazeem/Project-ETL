package com.azeem.blueprint.model.prediction;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PredictionRequest {
  @JsonProperty("historical_data")
  private List<DataPoint> historicalData;

  @JsonProperty("forecast_periods")
  private int forecastPeriods;

  public PredictionRequest() {}

  public PredictionRequest(List<DataPoint> historicalData, int forecastPeriods) {
    this.historicalData = historicalData;
    this.forecastPeriods = forecastPeriods;
  }

  public List<DataPoint> getHistoricalData() {
    return historicalData;
  }

  public void setHistoricalData(List<DataPoint> historicalData) {
    this.historicalData = historicalData;
  }

  public int getForecastPeriods() {
    return forecastPeriods;
  }

  public void setForecastPeriods(int forecastPeriods) {
    this.forecastPeriods = forecastPeriods;
  }
}
