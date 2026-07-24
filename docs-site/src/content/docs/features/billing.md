---
title: Billing Analytics
description: Billing record queries, summary analytics, and CSV export.
sidebar:
  order: 3
---

## Overview

Billing records are the core data type. After ingestion, they are queryable by period, provider, and top-N charge, with summary analytics computed on the fly.

## Dashboard Areas

- **Summary cards** — period-level totals (total charges, record count, top provider)
- **All records table** — paginated billing records with full detail
- **Provider chart** — Chart.js bar chart of charges by cloud provider
- **Cloud provider filter** — table filtered to a specific provider
- **Top N charges** — table of highest charges
- **CSV export** — download billing data as CSV

## API Endpoints

All endpoints are scoped under `/datasets/{datasetId}`:

| Endpoint | Frontend | Behavior |
|----------|----------|----------|
| `GET /records/periods` | Period dropdown | Returns `List<String>` |
| `GET /records/periods/{billingPeriod}` | Records table + chart | Paged records |
| `GET /summary/periods/{billingPeriod}` | Summary cards | `BillingSummary` |
| `GET /records/providers/{provider}` | Provider filter table | Paged, filtered |
| `GET /top/{n}` | Top N table | Validates `n` between 1 and 100 |
| `GET /records` | — | All records across periods |
| `GET /summary` | Predictions | Summary across all records |

## Key Files

| File | Role |
|------|------|
| `BillingController.java` | REST endpoints under `/datasets/{datasetId}` |
| `BillingQueryService.java` | Read-only billing queries and summaries |
| `BillingRecordRepository.java` | Spring Data JPA repository |
| `BillingRecordEntity.java` | JPA entity |
| `BillingRecord.java` | Domain model record |
| `BillingSummary.java` | Aggregate analytics model |
| `SummaryBuilder.java` | Computes summary from records |
| `CsvExportService.java` | CSV export service |

## Validation

- Period routes validate `YYYY-MM` format or the literal `dummy-data`
- Provider routes validate `@NotBlank` on the path variable
- `GET /top/{n}` validates `n` between 1 and 100 via `@Min`/`@Max`
