import boto3

from app.fetchers.aws_fetcher import AWSBillingFetcher
from app.fetchers.azure_fetcher import AzureBillingFetcher
from app.fetchers.gcp_fetcher import GCPBillingFetcher
from app.normalizer import BillingNormalizer


def run_ingestion_pipeline(tenant_id: str, customer_config: dict):
    print(f"Starting ingestion for tenant: {tenant_id}")

    # 1. Fetch raw data in parallel or sequentially
    aws_fetcher = AWSBillingFetcher(customer_config['aws_role_arn'], customer_config['aws_ext_id'])
    aws_bytes = aws_fetcher.fetch_latest_report(customer_config['aws_bucket'], "cur-report.csv")

    azure_fetcher = AzureBillingFetcher(customer_config['azure_creds'])
    azure_bytes = azure_fetcher.fetch_latest_report(customer_config['azure_container'], "export.csv")

    gcp_fetcher = GCPBillingFetcher(customer_config['gcp_sa_key'])
    gcp_bytes = gcp_fetcher.fetch_latest_report(customer_config['gcp_bucket'], "billing.csv")

    # 2. Normalize and Combine via Polars
    unified_csv_bytes = BillingNormalizer.aggregate_and_combine(
        aws_data=aws_bytes,
        azure_data=azure_bytes,
        gcp_data=gcp_bytes
    )

    # 3. Stream normalized CSV directly into App's S3 Bucket
    # (LocalStack in Dev, Real S3 in Prod)
    app_s3_client = boto3.client('s3')
    target_key = f"ingested-billing/{tenant_id}/latest.csv"
    
    app_s3_client.put_object(
        Bucket="cloud-billing", # The bucket your Java app watches via SQS!
        Key=target_key,
        Body=unified_csv_bytes,
        ContentType="text/csv"
    )

    print(f"Ingestion complete. Dropped normalized CSV to s3://cloud-billing/{target_key}")

if __name__ == "__main__":
    # Test execution
    dummy_config = { ... }
    run_ingestion_pipeline("org-1234", dummy_config)