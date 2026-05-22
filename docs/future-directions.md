## Future Architecture Considerations

These are natural next steps based on the current codebase. Items already implemented have been moved to the main documentation.

### blueprint-notifications — TypeScript Notification Microservice

The Spring Boot application already has a wired `NotificationClient` that dispatches alarm payloads to `POST /notify` on an external service at `${NOTIFICATION_SERVICE_URL:http://localhost:3000}`. The payload contract (`AlarmNotificationPayload`) is defined on the Java side. The microservice itself still needs to be built.

Suggested stack: Node.js, Express, TypeScript, AWS SES for email.

Scope for a first version:
- Accept `POST /notify` with the `AlarmNotificationPayload` array.
- Route each payload to at least one channel — email via SES is the most straightforward.
- Return 200 to Blueprint so the best-effort design stays clean.
- Log dispatched notifications.

This is a good first TypeScript service because the interface is fully defined by Blueprint: you implement one endpoint, one data structure, and one outbound call. Slack webhook and SMS channels can be added incrementally as you learn more.

Future expansions:
- Notification history stored in a small SQLite or Postgres database.
- Retry logic for failed deliveries.
- Per-user notification preferences (digest vs. real-time).
- Slack webhook integration alongside email.

### blueprint-export — TypeScript CSV/PDF Export Microservice

A lightweight Node.js/Express service that accepts billing or alarm data and returns downloadable reports. Blueprint would call it when a user requests a report download, or the service could poll Blueprint's API on a schedule.

Suggested scope for a first version:
- Accept a POST with billing records or alarm list as JSON.
- Return a CSV file as an attachment.

Learning value: file streaming, `Content-Disposition` headers, working with `fast-csv` or `csv-stringify`.

PDF export (using `pdfkit` or `puppeteer`) can be added once the CSV path works.

### blueprint-status — TypeScript Health and Status Microservice

A small Express service that aggregates health signals from Blueprint and surfaces them as a simple status page or JSON feed. Blueprint's `/actuator/health` is already exposed — this service would poll it and optionally track historical uptime.

Suggested scope for a first version:
- Poll Blueprint's actuator health endpoint on a timer.
- Expose a `GET /status` endpoint with current health and last-check timestamp.
- Optionally serve a minimal HTML status page.

Learning value: `setInterval`, `fetch` in Node, serving both JSON and HTML from the same Express app. Server-sent events (SSE) from this service would be a good next step — push live health updates to a browser without WebSockets.

### Harden AI Query Execution

- Replace string checks with SQL AST parsing (JSQLParser is the natural choice from the Java side).
- Enforce table and column allowlists.
- Enforce `billing_period` predicate programmatically.
- Add query timeout and row limit.
- Consider a read-only DB user for Martin queries.

### Add Real Authentication

The `AppUserEntity` and `AppUserRepository` already exist with fields for `provider`, `providerSubject`, `email`, `displayName`, and `role`. The scaffolding is in place for OAuth. What is missing is Spring Security, actual OAuth integration, and route protection.

Steps when the time comes:
- Add `spring-boot-starter-security`.
- Wire an OAuth 2.0 provider (Google is the natural fit given the existing Gemini/Spring AI dependency).
- Replace the hardcoded `X-User-Id` header with a resolved principal from the security context.
- Remove credential logging from `login.js`.
- Protect upload and Martin endpoints before public exposure.

### Optimize Bulk Ingestion Pipeline

- Replace the current parse → domain model → JPA entity flow with a two-path architecture.
- Retain JPA for transactional CRUD operations and single-record business logic.
- Introduce a dedicated bulk path using PostgreSQL `COPY` via `CopyManager` for mass CSV ingestion.
- Use `DataSourceUtils.getConnection()` instead of raw `DataSource.getConnection()` to keep bulk loads within Spring's transaction manager.
- Evaluate Spring Batch for chunked reads, parallel processing, and restart/retry on ingestion failure.
- Normalize the billing schema into `departments`, `employees`, and `billing_records` tables to reduce INSERT payload size and improve query performance.
- Disable indexes during bulk load and rebuild post-ingestion to eliminate per-row index maintenance overhead.

### Splunk SIEM Integration

- Ship structured application logs to Splunk via the HTTP Event Collector (HEC).
- Define log schemas for key pipeline events: ingestion start/end, row counts, Martin query execution, and AI response latency.
- Create Splunk alerts for anomalous ingestion volumes, query failures, and slow retrieval times.
- Tag all events with `billing_period`, `department`, and `employee_id` where applicable for correlation across dashboards.
- Explore Splunk SOAR integration to trigger automated responses on detected pipeline anomalies or security events.

The architecture already exports CloudWatch logs to a SIEM destination — this item is about defining the structured log schemas and alert rules that make the exported data actionable.

### Dataset Lifecycle Management

Currently datasets transition from `PENDING_INGESTION` to `READY` on successful ingestion. There is no delete, archive, or failure recovery flow exposed to the user.

- Expose `DELETE /datasets/{datasetId}` to remove a dataset and its billing records.
- Add a `FAILED` status and expose the error reason so the user can understand why a dataset did not ingest.
- Add `GET /datasets/{datasetId}/status` for polling ingestion progress without fetching the full dataset.
- Consider a webhook or SSE push from the ingestion listener so the frontend does not need to poll `waitForDataReady()`.
