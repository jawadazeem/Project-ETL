import polars as pl
from io import BytesIO

# Standard Schema Target: ServiceProviderChargeAccount
# Standard Columns: provider, charge_amount, service_name, usage_date, account_id

class BillingNormalizer:
    
    @staticmethod
    def normalize_aws(raw_bytes: bytes) -> pl.DataFrame:
        """Transforms AWS CUR / Billing CSV into standard schema."""
        df = pl.read_csv(BytesIO(raw_bytes))
        
        return df.select([
            pl.lit("AWS").alias("provider"),
            pl.col("lineItem/UnblendedCost").cast(pl.Float64).alias("charge_amount"),
            pl.col("lineItem/ProductCode").alias("service_name"),
            pl.col("lineItem/UsageStartDate").alias("usage_date"),
            pl.col("lineItem/UsageAccountId").alias("account_id")
        ])

    @staticmethod
    def normalize_azure(raw_bytes: bytes) -> pl.DataFrame:
        """Transforms Azure Cost Management Export into standard schema."""
        df = pl.read_csv(BytesIO(raw_bytes))
        
        return df.select([
            pl.lit("AZURE").alias("provider"),
            pl.col("CostInBillingCurrency").cast(pl.Float64).alias("charge_amount"),
            pl.col("MeterCategory").alias("service_name"),
            pl.col("Date").alias("usage_date"),
            pl.col("SubscriptionId").alias("account_id")
        ])

    @staticmethod
    def normalize_gcp(raw_bytes: bytes) -> pl.DataFrame:
        """Transforms GCP BigQuery/GCS Billing Export into standard schema."""
        df = pl.read_csv(BytesIO(raw_bytes))
        
        return df.select([
            pl.lit("GCP").alias("provider"),
            pl.col("cost").cast(pl.Float64).alias("charge_amount"),
            pl.col("service/description").alias("service_name"),
            pl.col("usage_start_time").alias("usage_date"),
            pl.col("project/id").alias("account_id")
        ])

    @classmethod
    def aggregate_and_combine(cls, aws_data: bytes, azure_data: bytes, gcp_data: bytes) -> bytes:
        """Combines all three clouds into a single unified CSV byte stream."""
        dfs = []
        
        if aws_data:
            dfs.append(cls.normalize_aws(aws_data))
        if azure_data:
            dfs.append(cls.normalize_azure(azure_data))
        if gcp_data:
            dfs.append(cls.normalize_gcp(gcp_data))
            
        if not dfs:
            raise ValueError("No cloud data was retrieved.")

        # Concatenate all dataframes vertically
        unified_df = pl.concat(dfs, how="vertical")
        
        # Write directly to in-memory CSV buffer (Zero Disk I/O)
        buffer = BytesIO()
        unified_df.write_csv(buffer)
        return buffer.getvalue()