package com.azeem.blueprint.controller;

import com.azeem.blueprint.model.prediction.AutoPredictionResponse;
import com.azeem.blueprint.model.prediction.PredictionResponse;
import com.azeem.blueprint.service.prediction.PredictionService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

  private final PredictionService predictionService;

  public PredictionController(PredictionService predictionService) {
    this.predictionService = predictionService;
  }

  // TODO: This needs to be USER scoped, not datasetID scoped. Later we established one DatasetID gets ONE billing period. This wasn't the case before.
  @PostMapping("/auto/{datasetId}")
  public ResponseEntity<AutoPredictionResponse> autoPredict(@PathVariable UUID datasetId) {
    AutoPredictionResponse response = predictionService.autoPrediction(datasetId);
    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<PredictionResponse> getPredictions(@RequestBody List<UUID> datasetIds) {
    try {
      PredictionResponse response = predictionService.predict(datasetIds);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
