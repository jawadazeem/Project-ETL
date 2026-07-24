---
title: Backend
description: Spring Boot monolith architecture — controllers, services, persistence, and background processing.
sidebar:
  order: 3
---

## Controller Layer

| Controller | Base Path | Responsibility |
|------------|-----------|----------------|
| `DatasetController` | `/datasets` | Dataset CRUD, lifecycle (archive/restore/delete), listing |
| `DemoController` | `/demo-dataset` | Demo data loading |
| `BillingController` | `/datasets/{datasetId}` | Records, summary, top N |
| `AlarmController` | `/datasets/{datasetId}` | Alarm retrieval by scope and period |
| `TraceController` | `/datasets/{datasetId}` | AI chat endpoint |
| `PdfController` | `/datasets/{datasetId}/reports` | PDF generation and download |
| `CorporateInfoController` | `/users/{userId}/corporate-info` | Corporate branding management |
| `PredictionController` | `/api/predictions` | Billing trend predictions |
| `NotificationProxyController` | `/api/notifications` | Proxies to notification microservice |
| `CloudConnectionController` | `/cloud-connections` | Cloud connection CRUD and sync |

## Service Layer

| Service | Responsibility |
|---------|----------------|
| `DatasetService` | Dataset creation, S3 upload orchestration, listing, archiving, restoration, deletion |
| `DemoDatasetLoader` | Classpath seed data ingestion into the fixed demo dataset |
| `AppUserService` | User provisioning and lookup |
| `BillingQueryService` | Read-only billing queries and summaries, dataset-scoped |
| `BillingIngestionService` | CSV stream ingestion, batching, persistence, alarm triggering |
| `BillingS3Service` | S3 object upload, download, error-log upload |
| `CsvExportService` | CSV export of billing data |
| `AlarmDetectionService` | Pure alarm detection rules |
| `AlarmService` | Alarm persistence, deduplication, read APIs, notification dispatch |
| `TraceService` | Gemini prompt orchestration, validation, SQL execution, explanation |
| `QueryExecutionService` | SQL execution through `JdbcTemplate` |
| `SchemaService` | Hardcoded schema string for AI prompts |
| `SqlValidationService` | Lightweight SQL validation |
| `RateLimiter` | Rate limiting for Trace queries |
| `PredictionService` | Proxies billing data to the Python prediction microservice |
| `PdfReportService` | PDF report generation orchestration |
| `LocalJavaPdfRenderer` | Pure Java PDF rendering (implements `PdfRenderer`) |
| `PdfStorageService` | S3 storage for generated PDFs |
| `CorporateInfoService` | Corporate branding CRUD |
| `CloudConnectionService` | Cloud connection CRUD, status/poll-frequency updates, sync triggering |
| `CredentialEncryptionService` | AES-256-GCM encryption/decryption for cloud credentials |

## Mapper Layer

| Mapper | Dependency | Notes |
|--------|-----------|-------|
| `DatasetMapper` | none | Entity to `Dataset` domain record (includes `archived` field) |
| `BillingRecordMapper` | `DatasetRepository` | Resolves dataset reference |
| `AlarmMapper` | `DatasetRepository` | Resolves dataset reference |
| `AppUserMapper` | none | Entity to `AppUser` domain record |
| `CorporateInfoMapper` | none | Entity to `CorporateInfo` domain record |
| `PdfReportMapper` | none | Entity to `PdfReport` domain record |
| `CloudConnectionMapper` | `AppUserRepository`, `CredentialEncryptionService` | Handles credential encryption/decryption |

## Notification Client

`NotificationClient` is a Spring `@Component` using `RestClient`:

- Accepts a list of `Alarm` domain objects
- Sends one `POST /notify` per alarm to the notification microservice
- Builds payloads matching the Zod schema with `alarmId`, `severity`, `title`, `message`, and `recipients`
- `AlarmService.notifyQuietly()` wraps every call in a try-catch — failures are logged as WARN and never propagate

## Persistence Layer

- Spring Data JPA repositories for all entities
- `JdbcTemplate` for AI-generated SQL execution (Trace queries only)
- `JdbcBillingBatchWriter` for high-performance raw JDBC batch inserts during ingestion
- Schema managed by **Liquibase** with ordered changesets

| Changeset | Creates |
|-----------|---------|
| `001-create-app-users.xml` | `app_users` table |
| `002-create-datasets.xml` | `datasets` table with FK to `app_users` |
| `003-create-billing-records.xml` | `billing_records` table with FK to `datasets` |
| `004-create-alarms.xml` | `alarms` table with FK to `datasets` |
| `005-add-performance-indexes.xml` | Performance indexes on billing records and alarms |
| `006-create-corporate-info.xml` | `corporate_info` table with FK to `app_users` |
| `007-create-pdf-reports.xml` | `pdf_reports` table with FK to `datasets` |
| `008-add-archived-to-datasets.xml` | `archived` boolean column on `datasets` |
| `013-create-cloud-connections.xml` | `cloud_connections` table with FK to `app_users` |

## Background Processing

- `BillingEventListener` uses `@SqsListener("billing-event-queue")`
- It expects S3 event JSON with `Records[0].s3.bucket.name` and `Records[0].s3.object.key`
- It downloads the object, resolves the dataset by S3 key, and ingests the CSV

No other scheduled jobs, worker processes, or async executors are currently active.
