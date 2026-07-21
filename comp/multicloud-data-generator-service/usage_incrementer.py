import numpy as np
import pandas as pd

class UsageIncrementer:
    def increment(self, url: str, scalar: float) -> pd.DataFrame:
        
        # AWS specific config. 
        if url.startswith("s3://"):
            storage_options = {
                "client_kwargs": {
                    "endpoint_url": "http://localhost:4568"
                },
                "key": "test",
                "secret": "test"
            }
            df = pd.read_csv(url, storage_options=storage_options)
        elif url.startswith("az://") or url.startswith("abfs://"): # Azure specific config. 
            azurite_conn_str = (
                "DefaultEndpointsProtocol=http;"
                "AccountName=devstoreaccount1;"
                "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
                "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
            )
            storage_options = {
                "connection_string": azurite_conn_str
            }
            df = pd.read_csv(url, storage_options=storage_options)
        else:
            df = pd.read_csv(url) # GCP doesn't need storage options
            
        numeric_cols = df.select_dtypes(include="number").columns
        df[numeric_cols] = df[numeric_cols] * scalar
        return df