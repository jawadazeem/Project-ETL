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

  @PostMapping("/auto")
  public ResponseEntity<AutoPredictionResponse> autoPredict(
      @RequestHeader("X-User-Id") String userId) {
    AutoPredictionResponse response = predictionService.autoPrediction(userId);
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
