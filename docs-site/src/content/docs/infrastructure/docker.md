---
title: Docker & Deployment
description: Docker Compose configuration, CI/CD pipeline, and infrastructure.
sidebar:
  order: 2
---

## Development Environment

```bash
docker compose --env-file .env -f docker-compose.dev.yml up --build
```

Services in `docker-compose.dev.yml`:
- **PostgreSQL 16** — database
- **LocalStack 3.0.0** — S3 and SQS emulation
- **App** — Spring Boot monolith (built from Dockerfile)
- **Notification service** — Node.js/Express/MongoDB
- **Prediction service** — Python/Flask
- **AWS CLI setup** — creates S3 bucket, SQS queue, and S3→SQS notification bridge

### Reset Database

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up --build
```

### Debug Logs

```bash
docker compose -f docker-compose.dev.yml logs app
docker compose -f docker-compose.dev.yml logs localstack
docker compose -f docker-compose.dev.yml logs aws-cli-setup
```

## Production

`docker-compose.prod.yml` uses the same pattern with a Cloudflare tunnel for secure routing.

## CI/CD

**Workflow**: `.github/workflows/docker-pipeline.yml`

- **Trigger**: push to `main`
- **Runner**: self-hosted
- Builds Docker images and deploys to AWS ECS

## Cloud Infrastructure

| Service | Purpose |
|---------|---------|
| **AWS ECS** | Container hosting |
| **AWS RDS** | PostgreSQL database |
| **AWS S3** | File storage (billing CSVs, PDFs, error logs) |
| **AWS SQS** | Event-driven ingestion queue |
| **AWS SES** | Email delivery for notifications |
| **AWS SNS** | Pub/sub messaging |
| **Cloudflare Tunnel** | Secure routing without exposed ports |

## Storage Layout

- **S3 bucket**: `cloud-billing`
- **Object key layout**: `ownerUserId/datasetId/filename.csv`
- **Error logs**: `error-logs/{billingPeriod}-errors.log`
- **PDF reports**: stored under generated S3 keys

## Observability

- Spring Boot Actuator is included
- `application.yaml` configures `logging.file.name: app.log`
- No metrics backend, tracing, or log aggregation configured yet
