---
title: Getting Started
description: Set up Blueprint for local development.
sidebar:
  order: 1
---

## Prerequisites

- **Java 25** (or compatible JDK)
- **Maven** 3.9+
- **Docker** and **Docker Compose**
- **Node.js** 20+ (for the notification microservice)
- **Python** 3.11+ (for the prediction microservice)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/jawadazeem/blueprint.git
cd blueprint

# Build the project
mvn clean package

# Run with Docker (recommended)
docker compose --env-file .env -f docker-compose.dev.yml up --build
```

The dashboard will be available at `http://localhost:8080`.

## Development Commands

```bash
# Build
mvn clean package

# Run tests
mvn test

# Apply code formatting (Google Java Format via Spotless)
mvn spotless:apply

# Check formatting without applying
mvn spotless:check
```

## Verify the Setup

```bash
# Health check
curl http://localhost:8080/actuator/health

# Load demo data
curl -X POST http://localhost:8080/demo-dataset

# List datasets
curl -H "X-User-Id: 00000000-0000-0000-0000-000000000001" \
     http://localhost:8080/datasets

# Check billing periods
curl http://localhost:8080/datasets/00000000-0000-0000-0000-000000000000/records/periods

# Get summary
curl http://localhost:8080/datasets/00000000-0000-0000-0000-000000000000/summary/periods/dummy-data
```

## Environment Variables

Key environment variables (configured in `.env` or `application.yaml`):

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SPRING_AI_GOOGLE_API_KEY` | Google Gemini API key for Trace |
| `NOTIFICATION_SERVICE_URL` | URL of the notification microservice |
| `CLOUD_CONNECTION_ENCRYPTION_KEY` | Base64-encoded 32-byte AES key for credential encryption |

## Code Style

Blueprint uses **Google Java Format** enforced by the Spotless Maven plugin. Always run `mvn spotless:apply` before committing — the build will fail on formatting violations.

## Live API

A live version of the API is deployed at **https://blueprint.jawadazeem.com**.
