## Future Architecture Considerations

These are natural next steps based on the current codebase. Items already implemented have been moved to the main documentation.

### Cloud Billing Ingestion Microservice

**Mission:** Pull billing data directly from customers' cloud storage and feed it into Blueprint's existing ingestion pipeline.

A standalone Flask or FastAPI microservice that connects to customer-owned cloud storage, extracts billing exports, normalizes them, and deposits the results into Blueprint's S3 bucket for processing via AWS Glue.

#### Architecture

```
Customer Buckets                  Ingestion Service              Blueprint Pipeline
┌──────────────┐                 ┌──────────────────┐           ┌──────────────────┐
│ AWS S3 (CUR) │───┐             │  Flask / FastAPI  │           │  Blueprint S3    │
├──────────────┤   │   pull      │                  │   deposit │  (cloud-billing) │
│ GCP Cloud    │───┼────────────>│  Extract          │──────────>│       │          │
│ Storage      │   │             │  Normalize        │           │       ▼          │
├──────────────┤   │             │  Validate         │           │  AWS Glue        │
│ Azure Blob   │───┘             │                  │           │       │          │
│ Storage      │                 └──────────────────┘           │       ▼          │
└──────────────┘                                                │  SQS → Monolith  │
                                                                └──────────────────┘
```

#### Data Flow

1. Customer configures cross-account access (IAM role for AWS, service account for GCP, SAS token for Azure) granting read-only access to their billing export bucket.
2. Ingestion service polls or receives webhook triggers when new billing exports land.
3. Service extracts raw billing data (AWS CUR Parquet/CSV, GCP BigQuery export JSON, Azure Cost Management CSV).
4. Each provider adapter normalizes to Blueprint's standard schema: `Account_Name, Resource_ID, Cloud_Provider, Billing_Period, Compute_Hours, Storage_GB_Used, API_Requests, Total_Charge, Service_Name, Description`.
5. Normalized CSV is deposited into Blueprint's S3 bucket at `{accountId}/{provider}/{YYYY-MM}/billing.csv`.
6. AWS Glue catalogs and transforms the raw data as needed.
7. The existing SQS event pipeline picks up the new object and the monolith ingests it through the standard `BillingEventListener` path.

#### Why a Separate Microservice

- **Language fit**: Python has first-class SDKs for all three cloud providers (`boto3`, `google-cloud-storage`, `azure-storage-blob`) and strong data processing libraries.
- **Isolation**: Ingestion failures, rate limits, or credential issues in one provider don't affect the monolith or other providers.
- **Independent scaling**: Can run on a schedule (cron) or event-driven, scaled separately from the main app.
- **Keeps the monolith unchanged**: Blueprint's S3→SQS→ingestion pipeline works exactly as it does today with manual CSV uploads.

### Account Cloud Provider Registry

Track which cloud providers each account uses. A new entity/table mapping users to their active cloud providers and account identifiers:

- AWS Account ID
- GCP Project ID
- Azure Subscription ID

This registry enables the Multi-Cloud Ingestion Pipeline to know which providers to poll for each account, and supports future per-provider dashboards and cost allocation views.

### Audit Agent

**Mission:** Find billing errors, anomalies, and cost inefficiencies before they impact the business.

#### Core Responsibilities

- Detect duplicate or overlapping charges across billing periods
- Identify unusual spending spikes and usage anomalies
- Flag idle or underutilized cloud resources that continue generating costs
- Detect unexpected cross-region traffic charges or premium-tier service usage
- Validate billing consistency across cloud providers, accounts, and services
- Identify missing credits, reserved instance discounts, or committed-use adjustments
- Surface recurring anomalies that indicate systemic billing issues

#### Investigation Workflow

1. Analyze newly ingested billing data
2. Compare charges against historical baselines
3. Cross-reference resource utilization against charges
4. Gather evidence supporting identified issues
5. Generate findings with confidence scores and estimated financial impact
6. Recommend escalation or remediation actions

#### Example Outputs

- "14 idle EC2 instances have incurred charges for 90 consecutive days with <1% CPU utilization."
- "Cross-region data transfer costs increased 340% compared to the previous six-month baseline."
- "Potential duplicate charge detected: identical resource billed under two AWS accounts."
- "Reserved instance coverage dropped to 45% — 12 on-demand instances could be converted."

---

### FinOps Agent

**Mission:** Optimize multi-cloud spending, forecast future costs, and continuously identify savings opportunities.

#### Core Responsibilities

- Identify cost reduction opportunities across cloud providers, services, and accounts
- Recommend actions to reduce future cloud expenses (rightsizing, reserved instances, spot usage)
- Forecast upcoming billing periods using historical trends and usage patterns
- Detect underutilized services, idle resources, and orphaned storage
- Prioritize savings opportunities based on projected financial impact
- Analyze spending efficiency across cloud providers and business units
- Model the impact of proposed optimization actions (e.g., moving workloads between providers)
- Generate executive-level cost optimization reports

#### Optimization Workflow

1. Analyze historical billing and usage trends across all cloud providers
2. Identify inefficiencies and optimization opportunities
3. Estimate potential savings and implementation effort
4. Rank recommendations by expected ROI
5. Simulate future cost scenarios
6. Produce actionable recommendations for stakeholders

#### Example Outputs

- "Rightsizing 37 over-provisioned EC2 instances could reduce annual compute spend by approximately $18,600."
- "Migrating low-traffic GCP Cloud Run services to Cloud Functions may save 12% annually."
- "Projected multi-cloud spend next quarter is expected to increase 9.4% due to seasonal usage patterns."
- "AWS and GCP account for 78% of spend growth over the last six months — Azure costs remain flat."

---

### RAG for Chat Interface

Augment Martin with a vector store of billing documentation, past analyses, and cloud provider pricing data. Retrieval-augmented generation would enable more contextual answers by grounding Martin's responses in:

- Historical billing analysis reports
- Cloud provider pricing documentation
- Internal cost allocation policies
- Past audit agent findings

This reduces hallucination risk and allows Martin to reference specific pricing tiers, discount programs, and organizational policies when answering questions.

---

### TensorFlow Migration

Replace scikit-learn linear regression in the prediction microservice with TensorFlow time-series models for more sophisticated forecasting. Candidates:

- LSTM networks for capturing seasonal billing patterns
- Prophet-style decomposition for trend + seasonality + holiday effects
- Transformer-based models for multi-variate cost prediction (incorporating compute hours, storage, API requests alongside total charge)

---

### Harden AI Query Execution

- Replace string checks with SQL AST parsing (JSQLParser is the natural choice from the Java side).
- Enforce table and column allowlists.
- Enforce `billing_period` predicate programmatically.
- Add query timeout and row limit.
- Consider a read-only DB user for Martin queries.

### Add Real Authentication

The `AppUserEntity` and `AppUserRepository` already exist with fields for `provider`, `providerSubject`, `email`, `displayName`, and `role`. The scaffolding is in place for OAuth. `SecurityConfig` currently permits all requests. What is missing is actual OAuth integration and route protection.

Steps when the time comes:
- Wire an OAuth 2.0 provider (Google is the natural fit given the existing Gemini/Spring AI dependency).
- Replace the hardcoded `X-User-Id` header with a resolved principal from the security context.
- Remove credential logging from `login.js`.
- Protect upload, Martin, and delete endpoints before public exposure.

### Splunk SIEM Integration

- Ship structured application logs to Splunk via the HTTP Event Collector (HEC).
- Define log schemas for key pipeline events: ingestion start/end, row counts, Martin query execution, and AI response latency.
- Create Splunk alerts for anomalous ingestion volumes, query failures, and slow retrieval times.
- Tag all events with `billing_period`, `cloud_provider`, and `resource_id` where applicable for correlation across dashboards.
- Explore Splunk SOAR integration to trigger automated responses on detected pipeline anomalies or security events.

### Dataset Status Improvements

Currently datasets transition from `PENDING_INGESTION` to `READY` on successful ingestion. Archive, restore, and delete are implemented. Remaining gaps:

- Add a `FAILED` status and expose the error reason so the user can understand why a dataset did not ingest.
- Add `GET /datasets/{datasetId}/status` for polling ingestion progress without fetching the full dataset.
- Consider a webhook or SSE push from the ingestion listener so the frontend does not need to poll `waitForDataReady()`.

### blueprint-status — TypeScript Health and Status Microservice

A small Express service that aggregates health signals from Blueprint and surfaces them as a simple status page or JSON feed. Blueprint's `/actuator/health` is already exposed — this service would poll it and optionally track historical uptime.

Suggested scope for a first version:
- Poll Blueprint's actuator health endpoint on a timer.
- Expose a `GET /status` endpoint with current health and last-check timestamp.
- Optionally serve a minimal HTML status page.

Learning value: `setInterval`, `fetch` in Node, serving both JSON and HTML from the same Express app. Server-sent events (SSE) from this service would be a good next step — push live health updates to a browser without WebSockets.

### Notification Microservice Enhancements

The notification microservice is implemented with email (SES) and Slack webhook delivery. Future improvements:

- Retry logic for failed deliveries with exponential backoff.
- Per-user notification preferences (digest vs. real-time).
- SMS channel via AWS SNS.
- Delivery status webhooks back to the monolith.

### Prediction Microservice Enhancements

The prediction microservice performs basic linear regression. Future improvements:

- Support additional models (ARIMA, exponential smoothing).
- Per-provider predictions in addition to aggregate.
- Confidence intervals on forecasted values.
- Auto-select latest billing data instead of manual dataset selection.
- Show forecasts for all providers on one dashboard view.
- Model training on historical data with persistent model storage.
