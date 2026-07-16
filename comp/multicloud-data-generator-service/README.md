# Multicloud Data Generator Service

Tiny Python based microservice running on separate containers from the main application. Responsible for generating cloud billing data in real time
on Localstack AWS, Localstack Azure, and LocalGCP. This is purely for demo/testing. In a real world environment, we would be acting on real data.

## Azure: Cost Management and Billing exports
- Stores in Localstack Azure container

## AWS: Cost and Usage Reports (CUR)
- Stores in Localstack AWS container

## GCP: Cloud Billing BigQuery Exports
- Stores in LocalGCP container