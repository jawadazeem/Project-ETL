---
title: Cloud Connections
description: Connect AWS, Azure, and GCP accounts with encrypted credentials for automated billing data polling.
sidebar:
  order: 8
---

## Overview

Cloud Connections allow users to register their cloud provider accounts so Blueprint can automatically fetch billing data on a configurable schedule. Credentials are encrypted at rest using AES-256-GCM.

## Architecture

The monolith owns everything — users, connections, credentials, scheduling. The Python ingestion service is a stateless worker: the monolith calls it with decrypted credentials and a target S3 key, it connects to the customer's cloud, fetches and normalizes the data, uploads the CSV to internal S3, and returns. The existing SQS pipeline handles the rest.

```
Monolith scheduler (checks which connections are due)
  → POST to ingestion service with decrypted creds, bucket info, target S3 key
    → Ingestion service connects to customer cloud, fetches, normalizes
    → Uploads normalized CSV to internal S3 at specified key
      → S3 event → SQS → BillingEventListener (existing pipeline, unchanged)
```

## API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/cloud-connections` | Create connection |
| `GET` | `/cloud-connections` | List user's connections |
| `GET` | `/cloud-connections/{id}` | Get single connection |
| `DELETE` | `/cloud-connections/{id}` | Delete connection |
| `PATCH` | `/cloud-connections/{id}/status` | Activate/deactivate |
| `PATCH` | `/cloud-connections/{id}/poll-frequency` | Change poll schedule |
| `POST` | `/cloud-connections/sync` | Trigger immediate sync |

## Credential Encryption

Credentials are encrypted using **AES-256-GCM** with a 32-byte key from the `CLOUD_CONNECTION_ENCRYPTION_KEY` environment variable.

- **Encrypt**: serialize credential map to JSON → generate 12-byte random IV → AES-GCM encrypt → prepend IV to ciphertext → Base64 encode
- **Decrypt**: Base64 decode → extract 12-byte IV → AES-GCM decrypt → deserialize JSON to map
- All crypto is JDK built-in (`javax.crypto`), no additional dependencies

### Provider Credential Keys

| Provider | Expected Keys |
|----------|---------------|
| AWS | `roleArn`, `externalId` |
| Azure | `tenantId`, `clientId`, `clientSecret`, `storageAccount`, `containerName` |
| GCP | `projectId`, `credentialsJson` |

## Domain Models

| Model | Purpose |
|-------|---------|
| `CloudConnectionRequest` | Request DTO with provider, display name, bucket, region, poll frequency, credentials |
| `CloudConnection` | Public response — all fields except credentials (never exposed) |
| `ActiveCloudConnection` | Internal model with decrypted credentials, used by scheduler/ingestion |
| `CloudConnectionStatus` | Enum: `ACTIVE`, `INACTIVE`, `ERROR` |

## Frontend

The Connections modal is accessible from the dashboard toolbar:

- **View** existing connections with provider badge, status indicator, and poll frequency
- **Add** new connections with provider-specific credential forms
- **Delete** connections with confirmation
- **Sync Now** button triggers immediate data pull for all active connections

## Key Files

| File | Role |
|------|------|
| `CloudConnectionController.java` | REST endpoints at `/cloud-connections` |
| `CloudConnectionService.java` | CRUD, status/poll-frequency updates, sync triggering |
| `CredentialEncryptionService.java` | AES-256-GCM encryption/decryption |
| `CloudConnectionMapper.java` | Entity/domain mapping with credential handling |
| `CloudConnectionRepository.java` | JPA repository with ownership-scoped queries |
| `CloudConnectionEntity.java` | JPA entity for `cloud_connections` table |
| `013-create-cloud-connections.xml` | Liquibase migration |
