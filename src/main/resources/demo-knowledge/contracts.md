# Cloud Provider Contracts — Azeem Corporation

## AWS Enterprise Agreement

- **Agreement ID:** EA-2025-AZM-4471
- **Term:** January 2025 - December 2027 (3-year)
- **Committed Annual Spend:** $1,600,000
- **Discount Tier:** Enterprise tier - 12% flat discount on all on-demand compute
- **Support Plan:** Business Support ($15,000/year)

### Negotiated Rates

| Service | Rate | Unit | Notes |
|---|---|---|---|
| EC2 (m5.xlarge) | $0.142/hr | per instance-hour | 18% below on-demand list |
| EC2 (m5.large) | $0.071/hr | per instance-hour | 18% below on-demand list |
| ECS (Fargate vCPU) | $0.03238/hr | per vCPU-hour | 10% below on-demand list |
| EKS (cluster fee) | $0.10/hr | per cluster-hour | Standard pricing |
| S3 Standard | $0.021/GB | per GB-month | First 500 TB |
| S3 Standard | $0.019/GB | per GB-month | 500 TB - 2 PB |
| Lambda | $0.0000166/GB-s | per GB-second | Standard pricing, no discount |
| RDS PostgreSQL (db.r5.large) | $0.185/hr | per instance-hour | 1-year reserved, all upfront |
| DynamoDB (on-demand reads) | $1.25 | per million read units | Standard pricing |
| ElastiCache (cache.r5.large) | $0.166/hr | per node-hour | Standard pricing |
| CloudFront | $0.085/GB | per GB transferred | First 10 TB/month |
| Kinesis Data Streams | $0.015/hr | per shard-hour | Standard pricing |
| Redshift (dc2.large) | $0.25/hr | per node-hour | Standard pricing |
| SQS | $0.40 | per million requests | Standard pricing |
| SNS | $0.50 | per million publishes | Standard pricing |
| NAT Gateway | $0.045/hr | per NAT-hour | Standard pricing |
| CloudWatch Logs | $0.50/GB | per GB ingested | Standard pricing |
| Data Transfer Out | $0.085/GB | per GB | First 10 TB/month |

### Reserved Instances

| Resource | Type | Count | Used By | Expiry |
|---|---|---|---|---|
| EC2 m5.xlarge | 1-year All Upfront | 24 | Production-Main, Edge-Network | March 2026 |
| EC2 m5.large | 1-year All Upfront | 48 | Production-Main, Production-Secondary, ML-Training | March 2026 |
| RDS db.r5.large | 1-year All Upfront | 8 | Production-Main, Finance-Systems, Partner-Integration | June 2026 |
| ElastiCache cache.r5.large | 1-year All Upfront | 6 | Production-Main, Edge-Network, Marketing-Platform | September 2026 |

**Action item:** RI renewals for EC2 fleet due March 2026. Evaluate whether to convert to Savings Plans based on usage flexibility needs. ML-Training GPU instances (in ML-Training account) are currently all on-demand - evaluate reserved capacity.

### AWS Accounts Covered

Production-Main, Production-Secondary, Shared-Services, Edge-Network, QA-Testing, ML-Training, Compliance-Audit, Security-Ops, DevOps-Central, Internal-Tools, Customer-Portal, Mobile-Backend, Partner-Integration, Marketing-Platform, Data-Science, Finance-Systems

## GCP Enterprise Agreement

- **Agreement ID:** GCP-AZM-2025-887
- **Term:** March 2025 - February 2028 (3-year)
- **Committed Annual Spend:** $620,000
- **Committed Use Discount (CUD):** 3-year CUD on Compute Engine - 57% discount on committed vCPUs/memory
- **Support Plan:** Premium Support

### Negotiated Rates

| Service | Rate | Unit | Notes |
|---|---|---|---|
| Compute Engine (n2-standard-4) | $0.0712/hr | per instance-hour | CUD pricing |
| GKE (cluster mgmt) | $0.10/hr | per cluster-hour | Standard pricing |
| BigQuery | $5.00/TB | per TB scanned | On-demand pricing, no discount |
| Cloud SQL (db-standard-4) | $0.1836/hr | per instance-hour | Standard pricing |
| Cloud Spanner (per node) | $0.90/hr | per node-hour | Standard pricing |
| Cloud Storage (Standard) | $0.020/GB | per GB-month | Standard pricing |
| Cloud Run | $0.00002400/vCPU-s | per vCPU-second | Standard pricing |
| Cloud Functions | $0.0000025/invocation | per invocation | Standard pricing |
| Dataflow | $0.056/hr | per vCPU-hour | Standard pricing |
| Vertex AI (prediction) | $0.0538/hr | per node-hour | Standard pricing |
| Pub/Sub | $40/TB | per TB published | Standard pricing |
| Cloud DNS | $0.20/zone | per managed zone/month | Standard pricing |
| Memorystore (Redis, M1) | $0.049/GB/hr | per GB-hour | Standard pricing |
| Firestore | $0.18 | per 100K reads | Standard pricing |

### Sustained Use Discounts

GCP automatically applies sustained use discounts for Compute Engine instances running more than 25% of the month. Combined with CUDs, effective compute rates can drop to ~$0.035/hr for n2-standard-4. This primarily benefits Analytics-Platform, IoT-Platform, and Staging-Env accounts.

### GCP Accounts Covered

Analytics-Platform, Data-Science, Staging-Env, IoT-Platform, Customer-Portal, Edge-Network, Marketing-Platform, Security-Ops, Finance-Systems, Shared-Services, Partner-Integration, Production-Main, Production-Secondary, Internal-Tools

## Azure Enterprise Agreement

- **Agreement ID:** MS-EA-AZM-2026-112
- **Term:** January 2026 - December 2026 (1-year, pilot)
- **Committed Annual Spend:** $320,000
- **Discount Tier:** 5% EA discount on all consumption
- **Support Plan:** Standard Support

### Negotiated Rates

| Service | Rate | Unit | Notes |
|---|---|---|---|
| Virtual Machines (D4s v5) | $0.168/hr | per instance-hour | 5% EA discount applied |
| AKS (cluster mgmt) | Free | per cluster | Control plane is free |
| Blob Storage (Hot) | $0.018/GB | per GB-month | 5% EA discount applied |
| Azure SQL Database (S3) | $0.1936/hr | per DTU-hour | Standard pricing |
| Cosmos DB (RU/s) | $0.008/hr | per 100 RU/s | Standard pricing |
| Azure Functions | $0.000016/GB-s | per GB-second | Consumption plan |
| App Service (P1v3) | $0.115/hr | per instance-hour | 5% EA discount applied |
| Front Door | $0.01/GB | per GB transferred | Standard pricing |
| Event Hubs (Standard) | $0.028/hr | per throughput unit | Standard pricing |
| Azure Cache for Redis (C1) | $0.055/hr | per instance-hour | Standard pricing |
| Microsoft Sentinel | $2.46/GB | per GB ingested | Standard pricing |
| Key Vault | $0.03 | per 10K operations | Standard pricing |
| Azure Monitor | $2.30/GB | per GB ingested | Standard pricing |
| Service Bus (Standard) | $0.05 | per million operations | Standard pricing |
| Azure DevOps | $6.00 | per user/month | Basic plan |

**Note:** Azure is a growth engagement. Primary production runs on AWS and GCP. Azure is used for DR (DR-Recovery), DevOps tooling (DevOps-Central), security monitoring (Security-Ops via Microsoft Sentinel), and select customer-facing workloads (Customer-Portal, Mobile-Backend AKS clusters).

### Azure Accounts Covered

DR-Recovery, DevOps-Central, Customer-Portal, Production-Main, Production-Secondary, Security-Ops, Finance-Systems, Mobile-Backend, Data-Science, Shared-Services, Partner-Integration, Internal-Tools

## Renewal Calendar

| Provider | Agreement | Renewal Date | Action Required By |
|---|---|---|---|
| AWS | EA-2025-AZM-4471 | December 2027 | September 2027 |
| AWS | EC2 Reserved Instances | March 2026 | January 2026 |
| AWS | RDS Reserved Instances | June 2026 | April 2026 |
| AWS | ElastiCache Reserved Nodes | September 2026 | July 2026 |
| GCP | GCP-AZM-2025-887 | February 2028 | November 2027 |
| Azure | MS-EA-AZM-2026-112 | December 2026 | October 2026 |
