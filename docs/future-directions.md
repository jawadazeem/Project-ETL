## Future Architecture Considerations

These are natural next steps based on the current codebase. Items already implemented have been moved to the main documentation.

### Audit Agent

**Mission:** Find billing errors, anomalies, contract violations, and compliance issues before they impact the business.

#### Core Responsibilities

- Detect duplicate or overlapping charges across billing periods
- Identify unusual spending spikes and usage anomalies
- Compare contract rates against invoice rates and negotiated pricing
- Flag inactive or low-usage lines that continue generating costs
- Detect unexpected roaming, international, or premium-service charges
- Validate billing consistency across departments, accounts, and vendors
- Identify missing credits, discounts, or contractual adjustments
- Surface recurring anomalies that indicate systemic billing issues

#### Investigation Workflow

1. Analyze newly ingested billing data
2. Compare charges against historical baselines
3. Validate invoice data against contract terms
4. Gather evidence supporting identified issues
5. Generate findings with confidence scores and estimated financial impact
6. Recommend escalation or remediation actions

#### Example Outputs

- "14 inactive mobile lines have incurred charges for 90 consecutive days."
- "Invoice rate exceeds contracted rate by 18% for 27 accounts."
- "International roaming costs increased 340% compared to the previous six-month baseline."
- "Potential duplicate charge detected across multiple billing periods."

---

### FinOps Agent

**Mission:** Optimize telecom spending, forecast future costs, and continuously identify savings opportunities.

#### Core Responsibilities

- Identify cost reduction opportunities across departments and accounts
- Recommend actions to reduce future telecom expenses
- Forecast upcoming billing periods using historical trends and usage patterns
- Detect underutilized services and subscriptions
- Prioritize savings opportunities based on projected financial impact
- Analyze spending efficiency across departments and business units
- Model the impact of proposed optimization actions
- Generate executive-level cost optimization reports

#### Optimization Workflow

1. Analyze historical billing and usage trends
2. Identify inefficiencies and optimization opportunities
3. Estimate potential savings and implementation effort
4. Rank recommendations by expected ROI
5. Simulate future cost scenarios
6. Produce actionable recommendations for stakeholders

#### Example Outputs

- "Cancelling 37 inactive lines could reduce annual spend by approximately $18,600."
- "Migrating low-usage accounts to a lower-cost plan may save 12% annually."
- "Projected telecom spend next quarter is expected to increase 9.4% due to seasonal usage patterns."
- "Sales and Operations account for 78% of spend growth over the last six months."

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
- Tag all events with `billing_period`, `department`, and `employee_id` where applicable for correlation across dashboards.
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
- Department-level predictions in addition to aggregate.
- Confidence intervals on forecasted values.
- Model training on historical data with persistent model storage.
