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

### Tenant-Scoped RAG Knowledge Base

**Mission:** Let each tenant upload organizational context (contracts, policies, team structures) that Trace uses alongside billing data to produce grounded, business-aware answers.

#### The Problem

Trace currently answers questions using only the billing data in PostgreSQL. It can tell you "S3 costs are $12,000 this month" but it can't tell you "S3 costs are 40% above your contracted rate" — because it doesn't know your contracted rate. The missing layer is tenant-specific business context that lives outside the billing records.

#### What Tenants Upload

Markdown files containing organizational knowledge that rarely changes:

- **Contracts and pricing** — negotiated rates, reserved instance commitments, enterprise discount programs, committed-use agreements.
- **Organizational structure** — which teams own which cloud accounts/projects/subscriptions, cost center mappings, budget owners.
- **Cost policies** — approval thresholds, spending limits by team, tagging requirements, chargeback rules.
- **Infrastructure context** — known migrations ("moved from Azure VMs to GCP Cloud Run in March 2026"), seasonal patterns ("Q4 spend spikes are expected due to Black Friday scaling"), legacy systems.
- **Vendor context** — multi-year agreement terms, renewal dates, volume discount tiers.

#### Storage: S3 with Tenant Isolation

Knowledge files are stored in the existing S3 bucket with tenant-scoped paths:

```
s3://cloud-billing/knowledge/{ownerUserId}/contracts.md
s3://cloud-billing/knowledge/{ownerUserId}/org-structure.md
s3://cloud-billing/knowledge/{ownerUserId}/cost-policies.md
```

This follows the same `ownerUserId`-scoped pattern used for billing CSV uploads (`ownerUserId/datasetId/filename.csv`). No new bucket or infrastructure needed.

#### API

| Method | Path | Description |
|---|---|---|
| `POST` | `/users/{userId}/knowledge` | Upload a markdown knowledge file. Stored in S3. |
| `GET` | `/users/{userId}/knowledge` | List uploaded knowledge files (name, size, last modified). |
| `GET` | `/users/{userId}/knowledge/{filename}` | Download a specific knowledge file. |
| `DELETE` | `/users/{userId}/knowledge/{filename}` | Remove a knowledge file. |

Same controller pattern as the existing `CorporateInfoController` and `DatasetController`.

#### Trace Integration

When Trace receives a question:

1. Resolve `tenantId` from the dataset's `ownerUserId`.
2. Pull the tenant's knowledge files from S3.
3. Chunk and embed the markdown content (vector store — pgvector in PostgreSQL, or a lightweight in-memory index for demo scale).
4. Retrieve chunks relevant to the user's question via similarity search.
5. Inject retrieved context into Trace's Gemini prompt alongside the database schema and billing period.
6. Trace's answers are now grounded in both the billing data *and* the tenant's business context.

#### Example Impact

Without RAG:
> "Your AWS S3 costs were $12,000 in January."

With tenant knowledge (contracts.md says S3 contracted rate is $0.021/GB):
> "Your AWS S3 costs were $12,000 in January — 38% above your contracted rate of $0.021/GB. At current storage levels (420 TB), your expected cost should be ~$8,820. This may indicate uncontracted usage or a rate change worth reviewing."

#### Frontend

A "Knowledge Base" section in settings where users can upload, view, and delete their markdown context files. Same upload pattern as CSV datasets — multipart form data, stored in S3, listed in a table.

---

### TensorFlow Integration

Two concrete integration points where TensorFlow replaces naive approaches with models that actually learn from the data.

#### 1. Prediction Service: scikit-learn → TensorFlow LSTM

The current prediction microservice fits a straight line through billing data. Cloud costs are not linear — they have seasonality (quarter-end spikes, annual renewals), step functions (new services spun up mid-month), and provider-specific patterns that linear regression cannot represent.

**What changes:**

- Replace `LinearRegression` in `app.py` with a TensorFlow `Sequential` model using LSTM layers.
- LSTM (Long Short-Term Memory) networks learn temporal dependencies: "costs tend to spike in December" or "storage growth is accelerating" — patterns that exist in the shape of the sequence, not just the slope.
- The Flask/FastAPI service stays the same. The `/predict` endpoint still takes `historicalData` and returns `predictions`. Only the model behind it changes.

**Learning path (approachable, not deep):**

1. `tf.keras.Sequential` with one `LSTM(64)` layer and one `Dense(1)` output — that's the whole model.
2. Reshape billing data into sliding windows (e.g., use 6 months to predict month 7).
3. `model.fit()` trains it. `model.predict()` runs it. Same flow as scikit-learn, just with tensors instead of numpy arrays.
4. Save trained models with `model.save()` so you're not retraining on every request.

**Stretch goals once the basics work:**

- Multi-variate input: feed compute hours, storage GB, and API requests alongside total charge — LSTM handles multiple features naturally.
- Per-provider models: train separate models for AWS, GCP, Azure since their cost patterns differ.
- Confidence intervals via Monte Carlo dropout.

#### 2. Alarm Detection: Thresholds → TensorFlow Autoencoder

The current `AlarmDetectionService` fires when `charge > configuredLimit`. This catches obvious spikes but misses the patterns that actually cost organizations money: slow drift, seasonal false positives, and cross-service cost shifts.

**What changes:**

- Train a TensorFlow autoencoder on "normal" billing patterns per provider/service.
- An autoencoder learns to compress and reconstruct normal data. When it sees abnormal data, reconstruction error spikes — that's the anomaly signal.
- This runs as a second detection pass alongside existing thresholds, not replacing them. Thresholds catch the obvious; the autoencoder catches the subtle.

**Concrete examples of what this catches that thresholds miss:**

- A service creeping up 5% month-over-month for 8 months (no single month triggers a threshold, but the trend is anomalous).
- Seasonal spending that *should* spike in Q4 — thresholds would fire a false alarm; the autoencoder knows December is expensive.
- Cost shifts between services: compute drops but storage rises by the same amount — total charge unchanged, but something changed underneath.

**Architecture:**

```
BillingIngestionService
    │
    ├── AlarmDetectionService (existing thresholds — unchanged)
    │
    └── AnomalyDetectionClient (new) ──HTTP──> anomaly-service (Python/TensorFlow)
                                                    │
                                                    ├── Autoencoder model per provider
                                                    ├── Returns anomaly scores + explanations
                                                    └── Trained offline on historical data
```

**Learning path:**

1. `tf.keras.Sequential` with encoder layers `Dense(64) → Dense(32) → Dense(16)` and mirror decoder `Dense(32) → Dense(64)` — compress then reconstruct.
2. Train on normal billing data: `model.fit(normal_data, normal_data)` — the model learns to reconstruct normal patterns.
3. At detection time: `reconstruction_error = |input - model.predict(input)|`. High error = anomaly.
4. Set a threshold on reconstruction error (e.g., 95th percentile of training errors) to convert continuous scores into alarm/no-alarm decisions.

**Integration with existing alarms:**

- The anomaly service returns scored results to the monolith via HTTP, same pattern as the prediction service.
- `AlarmService` merges threshold-based and anomaly-based alarms before persistence and notification dispatch.
- Anomaly-sourced alarms get a distinct `alarmType` (e.g., `ANOMALY_DRIFT`, `ANOMALY_SEASONAL`) so the frontend can distinguish them from threshold alarms.

---

### Harden AI Query Execution

- Replace string checks with SQL AST parsing (JSQLParser is the natural choice from the Java side).
- Enforce table and column allowlists.
- Enforce `billing_period` predicate programmatically.
- Add query timeout and row limit.
- Consider a read-only DB user for Trace queries.

### Add Real Authentication

The `AppUserEntity` and `AppUserRepository` already exist with fields for `provider`, `providerSubject`, `email`, `displayName`, and `role`. The scaffolding is in place for OAuth. `SecurityConfig` currently permits all requests. What is missing is actual OAuth integration and route protection.

Steps when the time comes:
- Wire an OAuth 2.0 provider (Google is the natural fit given the existing Gemini/Spring AI dependency).
- Replace the hardcoded `X-User-Id` header with a resolved principal from the security context.
- Remove credential logging from `login.js`.
- Protect upload, Trace, and delete endpoints before public exposure.

### Splunk SIEM Integration

- Ship structured application logs to Splunk via the HTTP Event Collector (HEC).
- Define log schemas for key pipeline events: ingestion start/end, row counts, Trace query execution, and AI response latency.
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
