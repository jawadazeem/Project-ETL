# CLAUDE.md

This file provides guidance to AI coding agents (specifically Claude Code, but also Codex and others) working in this repository.

---

## Project Overview

Blueprint is a Spring Boot application that performs ETL on large telecom billing datasets and exposes
the results through a REST API. It includes an autonomous AI agent ("Martin") that translates
natural language questions into validated PostgreSQL queries and returns plain-English answers.

A live version of the API is deployed on AWS at **https://blueprint.jawadazeem.com**.

---

## Tech Stack

- **Java 25 with Spring Boot 3
- **PostgreSQL** (production) / **H2** (tests)
- **Liquibase** for schema migrations
- **Google Gemini** via Spring AI for natural language → SQL
- **AWS**: ECS (hosting), RDS (database), S3 (file storage), SQS (event-driven ingestion), SNS
- **Docker** for containerisation
- **JUnit 5 + Mockito** for testing

---

## Development Commands

```bash
# Build the project
mvn clean package

# Run tests
mvn test

# Run with dev profile (after building) using Docker
./scripts/docker/dev.sh

# Apply code formatting (Google Java Format via Spotless)
mvn spotless:apply

# Check formatting without applying (runs automatically on validate phase)
mvn spotless:check
```

> Code formatting is enforced by the Spotless Maven plugin using Google Java Format.
> Always run `mvn spotless:apply` before committing, or the build will fail.

---

## Project Structure

```
src/main/java/com/azeem/blueprint/
├── config/          # Spring configuration beans (alarms, billing readers, etc.)
├── controller/      # REST controllers — thin layer, delegate to services
├── demo/            # Loads dummy data on startup (dev/demo only)
├── entity/          # JPA entities mapped to database tables
├── etl/             # CSV/TSV parsing and billing record assembly
├── exception/       # Custom exceptions and global exception handler
├── listener/        # SQS event listeners (event-driven ingestion)
├── mapper/          # Converts between entities and domain models
├── model/           # Domain model records/classes (not persisted directly)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic, organised by domain
│   ├── alarm/       # Alarm detection and persistence
│   ├── billing/     # Ingestion, S3 handling, querying
│   └── martin/      # AI agent: SQL generation, validation, execution
├── util/            # Shared utilities
└── validation/      # Custom Jakarta Bean Validation annotations and validators
```

---

## Domain Areas

**Billing** — ingests CSV/TSV datasets uploaded via S3/SQS, stores them as structured records in
PostgreSQL, and exposes query and summary endpoints.

**Alarms** — after ingestion, runs threshold-based detection across department totals, individual
charges, and account-level grand totals. Alarms are scoped by dataset and billing period.

**Martin (AI Agent)** — receives a natural language question, generates a validated read-only SQL
query using Gemini, executes it against the database, and returns a plain-English answer alongside
the SQL and its reasoning.

---

## Rules for AI Agents

### Do not modify these files without explicit instruction

These files are critical to deployment, infrastructure, or compliance and must not be changed
without the developer explicitly asking:

- `pom.xml` — dependency or build changes can break the pipeline
- `.github/workflows/` — CI/CD pipeline; changes affect live deployments
- `docker-compose.prod.yml` — production container configuration
- `Dockerfile` — container build definition
- `src/main/resources/db/migration/` — Liquibase migrations are irreversible once applied to production
- `LICENSE` — legal document

### Be cautious with these files

Changes here have broad impact across the application:

- `src/main/resources/application.yml` / `application-*.yml` — environment config and secrets wiring
- Any `*Config.java` in the `config/` package — modifying beans can have wide side effects
- `GlobalExceptionHandler.java` — changes affect all error responses across every endpoint

### Safe areas for AI agents

These areas are generally safe to work in without special caution:

- `src/test/` — adding or improving tests is always welcome
- `src/main/java/.../model/` — domain model changes are self-contained
- `src/main/java/.../service/` — business logic, but verify callers when changing method signatures
- `src/main/java/.../controller/` — REST layer, but don't change URL paths without checking clients
- `docs/` — documentation only

---

## Code Style

- Use **Google Java Format** (enforced by Spotless). Run `mvn spotless:apply` before committing.
- Follow existing patterns in the package you are editing. Do not introduce new frameworks or
  abstractions unless explicitly asked.
- Do not add comments explaining what the code does — name things well instead. Only add a comment
  when the *why* is non-obvious (a workaround, a hidden constraint, a surprising invariant).
- Do not add error handling for scenarios that cannot happen. Only validate at system boundaries
  (user input, external APIs). Trust Spring and JPA guarantees internally.

---

## Testing

- Unit tests use **JUnit 5 + Mockito** and run against **H2** (in-memory).
- Do not mock the database in integration tests — use the H2 test profile instead.
- Tests live in `src/test/java/` mirroring the main source structure.
- Run the full test suite with `mvn test` before marking any task complete.

---

## Deployment

Pushing to `main` triggers the GitHub Actions pipeline (`.github/workflows/docker-pipeline.yml`),
which builds a Docker image and deploys it to AWS ECS automatically. Do not push broken code to
`main`.
