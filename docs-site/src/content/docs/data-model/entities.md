---
title: Entities & Schema
description: JPA entities, domain models, and database schema managed by Liquibase.
---

## Dataset

**Domain model**: `model/dataset/Dataset.java`
**Entity**: `entity/DatasetEntity.java`

| Field | Type | Meaning |
|-------|------|---------|
| `id` | UUID | DB-generated primary key |
| `ownerUserId` | UUID | Reference to `AppUserEntity` |
| `billingPeriod` | String | Detected billing period from CSV |
| `sourceFilename` | String | Original uploaded filename |
| `s3ObjectKey` | String | S3 path: `ownerUserId/datasetId/filename.csv` |
| `uploadedAt` | Instant | Upload timestamp |
| `status` | String | `PENDING_INGESTION` or `READY` |
| `archived` | boolean | Soft-delete flag |

## BillingRecord

**Domain model**: `model/billing/BillingRecord.java`
**Entity**: `entity/BillingRecordEntity.java`

| Field | Type | Meaning |
|-------|------|---------|
| `datasetId` | UUID | Owning dataset |
| `accountName` | String | Billing account name |
| `resourceId` | String | Cloud resource identifier |
| `cloudProvider` | String | Cloud provider name (AWS, GCP, AZURE) |
| `billingPeriod` | String | Period, usually `YYYY-MM` |
| `computeHours` | double | Compute usage in hours |
| `storageGbUsed` | double | Storage usage in GB |
| `apiRequests` | long | API request count |
| `totalCharge` | double | Total charge |
| `serviceName` | String | Cloud service name |
| `description` | String | Human-readable description |

## Alarm

**Domain model**: `model/alarm/Alarm.java`
**Entity**: `entity/AlarmEntity.java`

| Field | Type | Meaning |
|-------|------|---------|
| `id` | UUID | DB-generated primary key |
| `datasetId` | UUID | Owning dataset |
| `businessKey` | UUID | Unique key for deduplication |
| `alarmScope` | enum | `RESOURCE`, `PROVIDER`, or `ACCOUNT` |
| `billingPeriod` | String | Associated period |
| `alarmType` | String | Human-readable type |
| `alarmSeverity` | enum | `LOW`, `MEDIUM`, or `HIGH` |
| `explanation` | String | Display explanation |
| `timestamp` | Instant | Detection time |
| `resourceId` | String? | Used for resource alarms |
| `serviceName` | String? | Cloud service name for resource alarms |
| `cloudProvider` | CloudProvider? | Used for provider alarms |

## CloudConnection

**Domain model**: `model/cloudconnection/CloudConnection.java`
**Entity**: `entity/CloudConnectionEntity.java`

| Field | Type | Meaning |
|-------|------|---------|
| `id` | UUID | DB-generated primary key |
| `ownerUserId` | UUID | FK to `app_users` |
| `provider` | String | `AWS`, `AZURE`, or `GCP` |
| `displayName` | String | User-given name |
| `bucketName` | String | Source bucket name |
| `region` | String? | Cloud region |
| `encryptedCredentials` | String | AES-256-GCM encrypted credential map |
| `status` | String | `ACTIVE`, `INACTIVE`, or `ERROR` |
| `pollFrequency` | String | `HOURLY`, `DAILY`, `WEEKLY`, or `MANUAL` |
| `lastPolledAt` | Instant? | Last successful poll time |
| `createdAt` | Instant | Creation timestamp |
| `updatedAt` | Instant? | Last update timestamp |

## AppUser

**Domain model**: `model/user/AppUser.java`
**Entity**: `entity/AppUserEntity.java`

| Field | Type | Meaning |
|-------|------|---------|
| `id` | UUID | DB-generated primary key |
| `provider` | String? | OAuth provider (not yet wired) |
| `providerSubject` | String? | OAuth subject |
| `email` | String? | User email |
| `displayName` | String? | Display name |
| `pictureUrl` | String? | Profile picture URL |
| `role` | String? | User role |
| `createdAt` | Instant | Creation timestamp |
| `lastLoginAt` | Instant? | Last login |

## CorporateInfo

**Entity**: `entity/CorporateInfoEntity.java`

Fields: `id`, `userId`, `companyName`, `address`, `phone`, `email`, `logoUrl`.

Used for PDF report branding.

## PdfReport

**Entity**: `entity/PdfReportEntity.java`

Fields: `id`, `datasetId`, `userId`, `billingPeriod`, `s3Key`, `generatedAt`.

## Prediction Models

| Model | Fields | Role |
|-------|--------|------|
| `DataPoint` | `period`, `totalCharge` | Historical billing data point |
| `PredictionRequest` | `historicalData`, `periodsToPredict` | Request to prediction microservice |
| `PredictionResponse` | `predictions`, `historicalData`, `model` | Response with forecasted values |

## Trace Models

| Model | Fields | Role |
|-------|--------|------|
| `TraceRequest` | `prompt`, `period` | Request body from frontend |
| `SqlResponse` | `sql`, `reasoning` | Expected Gemini JSON response |
| `TraceResponse` | `answer`, `sql`, `reasoning` | Response returned to frontend |
