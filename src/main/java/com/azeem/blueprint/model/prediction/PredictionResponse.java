package com.azeem.blueprint.model.prediction;

import java.util.List;

public class PredictionResponse {
  private List<DataPoint> predictions;

  public PredictionResponse() {}

  public PredictionResponse(List<DataPoint> predictions) {
    this.predictions = predictions;
  }

  public List<DataPoint> getPredictions() {
    return predictions;
  }

  public void setPredictions(List<DataPoint> predictions) {
    this.predictions = predictions;
  }
}
