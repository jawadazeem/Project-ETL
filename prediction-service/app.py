from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression

app = FastAPI(title="Prediction Service", version="1.0.0")

class DataPoint(BaseModel):
    period: str  # e.g., "2026-01"
    charge: float

class PredictionRequest(BaseModel):
    historical_data: List[DataPoint]
    forecast_periods: int = 3  # Default to predicting 3 periods into the future

class PredictionResponse(BaseModel):
    predictions: List[DataPoint]

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest):
    if len(request.historical_data) < 3:
        raise HTTPException(status_code=400, detail="At least 3 historical data points are required for prediction.")
    
    # Sort data by period to ensure order
    df = pd.DataFrame([dp.dict() for dp in request.historical_data])
    df = df.sort_values(by="period").reset_index(drop=True)
    
    # Simple feature engineering: using index as time variable
    # In a production system, we might parse dates and use relative days
    X = np.array(df.index).reshape(-1, 1)
    y = df['charge'].values
    
    model = LinearRegression()
    model.fit(X, y)
    
    # Generate future periods
    # Assuming periods are in YYYY-MM format, we'll just increment month
    last_period = df['period'].iloc[-1]
    
    try:
        year, month = map(int, last_period.split("-"))
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid period format. Expected YYYY-MM")
    
    future_data = []
    for i in range(1, request.forecast_periods + 1):
        # Calculate next period (YYYY-MM)
        next_month = month + i
        next_year = year + (next_month - 1) // 12
        next_month = (next_month - 1) % 12 + 1
        next_period_str = f"{next_year}-{next_month:02d}"
        
        # Predict
        # The index for future periods continues from len(X)
        pred_x = np.array([[len(X) - 1 + i]])
        pred_charge = model.predict(pred_x)[0]
        
        # Ensure we don't predict negative charges for billing
        pred_charge = max(0, pred_charge)
        
        future_data.append(DataPoint(period=next_period_str, charge=round(pred_charge, 2)))
        
    return PredictionResponse(predictions=future_data)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
