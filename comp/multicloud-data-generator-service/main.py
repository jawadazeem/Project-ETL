import time
import pandas as pd
from multi_cloud_storage_manager import MultiCloudStorageManager
from usage_incrementer import UsageIncrementer

def tick(manager: MultiCloudStorageManager,
         incrementer: UsageIncrementer,
         path_scalar_map: dict,
         bucket_name: str,
         period: str):
    
    for path, scalar in path_scalar_map.items():
        df = incrementer.increment(path, scalar)
        csv_bytes = df.to_csv(index=False).encode("utf-8")
    
        if path.startswith("s3://"):
            manager.s3_client.put_object(Bucket=bucket_name, Key=period, Body=csv_bytes)
        elif path.startswith("gs://"):
            bucket = manager.gcp_client.bucket(bucket_name)
            bucket.blob(period).upload_from_string(csv_bytes, content_type="text/csv")
        elif path.startswith("az://") or path.startswith("abfs://"):
            blob_client = manager.azure_client.get_blob_client(container=bucket_name, blob=period)
            blob_client.upload_blob(csv_bytes, overwrite=True)

def main():
    manager = MultiCloudStorageManager()
    incrementer = UsageIncrementer()

    container_client = manager.azure_client.get_container_client("billingreports")
    container_client.set_container_access_policy({}, public_access="blob")

    bucket_name = "billingreports"
    period = "2026-06.csv"
    manager.create_buckets(bucket_name)
    manager.upload("raw-data/aws_cur_2026-06.csv",
                   "raw-data/gcp_billing_2026-06.csv",
                   "raw-data/azure_costs_2026-06.csv",
                   bucket_name,
                   period)

    path_scalar_map = {}
    path_scalar_map["s3://billingreports/2026-06.csv"] = 1.05
    path_scalar_map["gs://billingreports/2026-06.csv"] = 1.05
    path_scalar_map["az://billingreports/2026-06.csv"] = 1.1

    interval = 5
    count = 0
    while True:
        tick(manager, incrementer, path_scalar_map, bucket_name, period)
        time.sleep(interval)
        count+=1
        print(f"Increment Count: {count}")

if __name__ == "__main__":
    main()
