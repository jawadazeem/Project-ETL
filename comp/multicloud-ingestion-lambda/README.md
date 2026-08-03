# Data Ingestion Service
## Responsibility: 
- To ingest billing data from AWS, Azure, and GCP multiple times a day, and forward it to the main application.
- Before forwarding it to the Java based monolith, it will adapt the data to the custom, vendor-agnostic format, similar to the FOCUS format.

## Infrastructure:
- This is a Lambda function that polls BLOB storages when hit with an EventBridge event sent from the main Java application.
- Stores the normalized data in the application's S3 bucket. The main Java application consumes this data via S3 event notifications.