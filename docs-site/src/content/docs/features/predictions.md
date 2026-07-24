---
title: Predictions
description: ML-powered billing trend forecasting using linear regression.
sidebar:
  order: 7
---

## Overview

Blueprint forecasts future billing charges based on historical data using scikit-learn linear regression.

## User Flow

1. User selects 3+ datasets from the prediction modal
2. Frontend sends `POST /api/predictions` with dataset IDs
3. Monolith gathers historical billing summaries and sends them to the prediction microservice
4. Prediction microservice runs linear regression and returns forecasted charges
5. Frontend renders a Chart.js line chart with historical and predicted values

## API

| Method | Path | Request | Response |
|--------|------|---------|----------|
| `POST` | `/api/predictions` | `List<UUID>` (dataset IDs) | `PredictionResponse` |

## Prediction Microservice

The `prediction-service/` is a standalone Python/Flask service:

- `POST /predict` — receives historical billing data, runs scikit-learn LinearRegression, returns forecasted charges
- Default forecast horizon: 3 periods
- Requires at least 3 datasets with valid billing periods

## Key Files

| File | Role |
|------|------|
| `PredictionController.java` | `POST /api/predictions` |
| `PredictionService.java` | Gathers data from datasets and proxies to Python service |
| `prediction-service/app.py` | Flask endpoint with scikit-learn linear regression |
| `DataPoint.java` | Historical billing data point model |
| `PredictionRequest.java` | Request to prediction microservice |
| `PredictionResponse.java` | Response with forecasted values |
