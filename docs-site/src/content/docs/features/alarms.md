---
title: Alarms & Notifications
description: Threshold-based anomaly detection and multi-channel notification delivery.
sidebar:
  order: 4
---

## Alarm Detection

After ingestion, Blueprint runs threshold-based detection across three scopes:

| Scope | Logic |
|-------|-------|
| `PROVIDER` | Cloud provider total exceeds configured monthly limit |
| `RESOURCE` | Individual resource charge exceeds low/medium/high thresholds |
| `ACCOUNT` | Grand total exceeds configured account thresholds |

### Configuration

Thresholds are defined in `application.yaml`:

```yaml
alarm:
  provider:
    monthlyLimit: 25000
  individual:
    low: 500
    medium: 2000
    high: 5000
  account:
    low: 100000
    high: 250000
```

## Notification Dispatch

After persisting new alarms, `AlarmService` calls `notifyQuietly()`, which dispatches each alarm to the notification microservice. Failures are caught and logged as `WARN` — they never propagate or roll back alarm persistence.

### Notification Payload

Each payload matches the Zod schema defined in the notification microservice:

| Field | Type | Notes |
|-------|------|-------|
| `alarmId` | String | UUID business key |
| `datasetId` | String | UUID |
| `billingPeriod` | String | e.g. `2026-01` |
| `severity` | String | `LOW`, `MEDIUM`, or `HIGH` |
| `title` | String | Human-readable alarm type |
| `message` | String | Explanation text |
| `recipients` | Object | `{ email: string[], slackWebhooks: string[] }` |

## Notification Microservice

The `notification-service/` is a standalone TypeScript/Express service:

- `POST /notify` — receives alarm payload, validates with Zod, dispatches via email (SES) and/or Slack, stores delivery record in MongoDB
- `GET /notifications?limit=N` — returns recent delivery history
- `public/` — serves a notification dashboard UI

The monolith proxies notification history through `NotificationProxyController` at `/api/notifications`.

## Frontend

- Dashboard calls `/datasets/{id}/alarms/{billingPeriod}` to update the red alarm button count
- Alarm modal lists all alarms for the selected period
- Alarm severity chart groups alarms by LOW, MEDIUM, HIGH, UNKNOWN
- Notifications modal shows delivery history fetched via `/api/notifications`

## Key Files

| File | Role |
|------|------|
| `AlarmDetectionService.java` | Pure detection logic over billing records |
| `AlarmService.java` | Persistence, deduplication, notification dispatch |
| `client/NotificationClient.java` | HTTP client for `POST /notify` |
| `AlarmController.java` | Alarm API endpoints |
| `notification-service/src/validation/notification.schema.ts` | Zod schema (contract source of truth) |
