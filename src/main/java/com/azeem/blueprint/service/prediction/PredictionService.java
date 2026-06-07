package com.azeem.blueprint.service.prediction;

import com.azeem.blueprint.model.billing.BillingSummary;
import com.azeem.blueprint.model.dataset.Dataset;
import com.azeem.blueprint.model.prediction.DataPoint;
import com.azeem.blueprint.model.prediction.PredictionRequest;
import com.azeem.blueprint.model.prediction.PredictionResponse;
import com.azeem.blueprint.service.billing.BillingQueryService;
import com.azeem.blueprint.service.dataset.DatasetService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PredictionService {

  private final DatasetService datasetService;
  private final BillingQueryService billingQueryService;
  private final RestTemplate restTemplate;
  private final String predictionServiceUrl;

  public PredictionService(
      DatasetService datasetService,
      BillingQueryService billingQueryService,
      @Value("${prediction.service.url:http://prediction-service:5000}")
          String predictionServiceUrl) {
    this.datasetService = datasetService;
    this.billingQueryService = billingQueryService;
    this.restTemplate = new RestTemplate();
    this.predictionServiceUrl = predictionServiceUrl;
  }

  public PredictionResponse predict(List<UUID> datasetIds) {
    if (datasetIds == null || datasetIds.size() < 3) {
      throw new IllegalArgumentException("At least 3 datasets must be selected for prediction.");
    }

    List<DataPoint> historicalData = new ArrayList<>();

    for (UUID id : datasetIds) {
      Dataset dataset = datasetService.getDataset(id);
      if (dataset == null || dataset.billingPeriod() == null) {
        continue;
      }
      BillingSummary summary = billingQueryService.generateSummary(id);
      historicalData.add(new DataPoint(dataset.billingPeriod(), summary.getTotalCharges()));
    }

    if (historicalData.size() < 3) {
      throw new IllegalArgumentException("Not enough valid historical data to perform prediction.");
    }

    // Sort by billing period (YYYY-MM)
    historicalData.sort(Comparator.comparing(DataPoint::getPeriod));

    PredictionRequest request = new PredictionRequest(historicalData, 3);

    String url = predictionServiceUrl + "/predict";
    return restTemplate.postForObject(url, request, PredictionResponse.class);
  }
}
