import numpy as np
import pandas as pd

class UsageIncrementer:
    def increment(url: str, scalar: float):
        df = pd.read_csv(url)
        numeric_cols = df.select_dtypes(include="number").columns
        df[numeric_cols] = df[numeric_cols] * scalar