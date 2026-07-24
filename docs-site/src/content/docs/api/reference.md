---
title: REST Endpoints
description: Complete API reference for all Blueprint REST endpoints.
---

OpenAPI/Swagger documentation is also available at `/swagger-ui.html` and `/v3/api-docs` when the server is running.

## Request & Response Patterns

- Controllers return JSON for API responses
- Paged endpoints return Spring `Page<T>` with `content`, page metadata, and sort metadata
- Error responses use `ErrorResponse` with `status`, `message`, and `timestamp`
- Validation uses Jakarta Bean Validation annotations and custom validators

---

## Datasets

| Method | Path | Header | Response |
|--------|------|--------|----------|
| `POST` | `/datasets` | `X-User-Id: String` | `Dataset` |
| `GET` | `/datasets` | `X-User-Id: UUID` | `List<Dataset>` (active only) |
| `GET` | `/datasets/{datasetId}` | — | `Dataset` |
| `DELETE` | `/datasets/{datasetId}` | `X-User-Id: UUID` | 204 No Content |
| `PATCH` | `/datasets/{datasetId}/archive` | `X-User-Id: UUID` | 204 No Content |
| `PATCH` | `/datasets/{datasetId}/restore` | `X-User-Id: UUID` | 204 No Content |
| `GET` | `/datasets/archived` | `X-User-Id: UUID` | `List<Dataset>` |

## Demo

| Method | Path | Response |
|--------|------|----------|
| `POST` | `/demo-dataset` | plain text |

## Billing Records

All under `/datasets/{datasetId}`:

| Method | Path | Params | Response | Validation |
|--------|------|--------|----------|------------|
| `GET` | `/records` | `page`, `size` | `Page<BillingRecord>` | — |
| `GET` | `/records/periods` | — | `List<String>` | — |
| `GET` | `/records/periods/{billingPeriod}` | `page`, `size` | `Page<BillingRecord>` | Custom period validator |
| `GET` | `/records/providers/{provider}` | `page`, `size`, `billingPeriod` | `Page<BillingRecord>` | `@NotBlank`, `@Min`, `@Max` |
| `GET` | `/summary` | — | `BillingSummary` | — |
| `GET` | `/summary/periods/{billingPeriod}` | — | `BillingSummary` | Custom period validator |
| `GET` | `/top/{n}` | — | `Page<BillingRecord>` | `@Min(1) @Max(100)` |

## Alarms

All under `/datasets/{datasetId}`:

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/alarms/{billingPeriod}` | `List<Alarm>` |
| `GET` | `/alarms/{billingPeriod}/provider` | `List<Alarm>` |
| `GET` | `/alarms/{billingPeriod}/resource` | `List<Alarm>` |
| `GET` | `/alarms/{billingPeriod}/account` | `List<Alarm>` |

## Trace

```http
POST /datasets/{datasetId}/trace
Content-Type: application/json

{
  "prompt": "Which cloud providers have the highest total charges?",
  "period": "dummy-data"
}
```

Response:
```json
{
  "answer": "...",
  "sql": "select ...",
  "reasoning": "..."
}
```

## PDF Reports

Under `/datasets/{datasetId}/reports`:

| Method | Path | Header | Response |
|--------|------|--------|----------|
| `POST` | `/pdf` | `X-User-Id: String` | `PdfReport` |
| `GET` | `/pdf/{reportId}` | — | PDF binary stream |

## Corporate Info

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/users/{userId}/corporate-info` | `CorporateInfo` or 404 |
| `PUT` | `/users/{userId}/corporate-info` | `CorporateInfo` |

## Predictions

| Method | Path | Request | Response |
|--------|------|---------|----------|
| `POST` | `/api/predictions` | `List<UUID>` (dataset IDs) | `PredictionResponse` |

## Notifications

| Method | Path | Params | Response |
|--------|------|--------|----------|
| `GET` | `/api/notifications` | `?limit=50` | JSON (proxied from notification service) |

## Cloud Connections

| Method | Path | Header | Response |
|--------|------|--------|----------|
| `POST` | `/cloud-connections` | `X-User-Id: UUID` | `CloudConnection` |
| `GET` | `/cloud-connections` | `X-User-Id: UUID` | `List<CloudConnection>` |
| `GET` | `/cloud-connections/{id}` | `X-User-Id: UUID` | `CloudConnection` |
| `DELETE` | `/cloud-connections/{id}` | `X-User-Id: UUID` | 204 No Content |
| `PATCH` | `/cloud-connections/{id}/status` | `X-User-Id: UUID` | `CloudConnection` |
| `PATCH` | `/cloud-connections/{id}/poll-frequency` | `X-User-Id: UUID` | `CloudConnection` |
| `POST` | `/cloud-connections/sync` | `X-User-Id: UUID` | `{connectionsQueued: N}` |
