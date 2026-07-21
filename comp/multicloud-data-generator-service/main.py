import time
import pandas as pd
from multi_cloud_storage_manager import MultiCloudStorageManager
from usage_incrementer import UsageIncrementer

def tick(manager: MultiCloudStorageManager,
         incrementer: UsageIncrementer,
         path_scalar_map: map,
         bucket_name: str,
         period: str):
    
    for path, scalar in path_scalar_map.items():
        df = incrementer.increment(path, scalar)
        csv_bytes = df.to_csv(index=False).encode('utf-8')
        manager.upload_csv_bytes(csv_bytes, bucket_name=bucket_name, object_name=period)

def main():
    manager = MultiCloudStorageManager()
    incrementer = UsageIncrementer()

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
