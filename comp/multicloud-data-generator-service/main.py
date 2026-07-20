import time
from multi_cloud_storage_manager import MultiCloudStorageManager
from usage_incrementer import UsageIncrementer

def tick(manager: MultiCloudStorageManager, incrementer: UsageIncrementer):
    incrementer.increment("s3://billing-report/2026-06.csv")
    incrementer.increment("gs://billing-report/2026-06.csv")
    incrementer.increment("az://billing-report/2026-06.csv")



def main():
    manager = MultiCloudStorageManager()
    incrementer = UsageIncrementer()
    manager.create_bucket_aws("billing-reports")
    manager.create_bucket_azure("billingreports")
    manager.create_bucket_gcp("billingreports")
    manager.upload_to_aws("raw-data/aws_cur_2026-06.csv", "billing-reports", "2026-06.csv")
    manager.upload_to_azure("raw-data/azure_costs_2026-06.csv", "billingreports", "2026-06.csv")
    manager.upload_to_gcp("raw-data/gcp_billing_2026-06.csv", "billingreports", "2026-06.csv")


    interval = 5
    while True:
        tick(manager, incrementer)
        time.sleep(interval)

if __name__ == "__main__":
    main()
