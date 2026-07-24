---
title: System Overview
description: High-level architecture and service interactions.
sidebar:
  order: 1
---

## High-Level Architecture

```mermaid
flowchart LR
    StaticUI["Static UI\nHTML/CSS/JS"] --> Controllers["Spring REST Controllers"]
    Controllers --> DatasetService["DatasetService"]
    Controllers --> BillingService["BillingQueryService"]
    Controllers --> AlarmService["AlarmService"]
    Controllers --> TraceService["TraceService"]
    Controllers --> PdfService["PdfReportService"]
    Controllers --> PredService["PredictionService"]
    Controllers --> NotifProxy["NotificationProxyController"]
    DatasetService --> S3Service["BillingS3Service"]
    S3Service --> S3["S3 / LocalStack"]
    S3 --> SQS["SQS event queue"]
    SQS --> Listener["BillingEventListener"]
    Listener --> Ingestion["BillingIngestionService"]
    Ingestion --> Repository["Spring Data JPA repositories"]
    BillingService --> Repository
    AlarmService --> Repository
    AlarmService --> NotifClient["NotificationClient"]
    NotifClient --> NotifService["notification-service\n(TS/Express/MongoDB)"]
    NotifProxy --> NotifClient
    PredService --> PredMicro["prediction-service\n(Python/Flask)"]
    PdfService --> S3
    TraceService --> JDBC["JdbcTemplate"]
    TraceService --> Gemini["Google Gemini via Spring AI"]
    Repository --> DB["PostgreSQL\n(Liquibase schema)"]
    JDBC --> DB
```

## Primary Workflows

```mermaid
flowchart TD
    A["Open dashboard"] --> B{"Data already loaded?"}
    B -->|No| C["Welcome overlay — upload CSV or load demo"]
    C --> D["Dataset created, CSV uploaded to S3"]
    D --> E["SQS triggers ingestion"]
    E --> F["Persist billing records"]
    F --> G["Detect and persist alarms"]
    G --> G2["Dispatch alarm notifications"]
    B -->|Yes| H["Silently pick first READY dataset"]
    G2 --> I["Select billing period"]
    H --> I
    I --> J["View summary cards"]
    I --> K["Browse paginated records"]
    I --> L["View charge and alarm charts"]
    I --> M["Filter by cloud provider"]
    I --> N["Top N charge lookup"]
    I --> O["Ask Trace"]
    I --> P2["Generate PDF report"]
    I --> P3["Run billing predictions"]
    I --> P4["View notification history"]
    I --> P5["Archive or delete dataset"]
    O --> P["Generate SQL with Gemini"]
    P --> Q["Validate SQL"]
    Q --> R["Execute query"]
    R --> S["Generate natural-language answer"]
```

## Repository Map

```
blueprint/
├── notification-service/     # TypeScript notification microservice (Express/MongoDB)
│   ├── src/                  # TypeScript source (server, controllers, services, models)
│   └── public/               # Notification dashboard UI
├── prediction-service/       # Python prediction microservice (Flask/scikit-learn)
│   ├── app.py                # Flask app with /predict endpoint
│   └── Dockerfile
├── docs-site/                # This documentation site (Astro/Starlight)
├── scripts/                  # Docker and JAR run scripts
└── src/main/java/com/azeem/blueprint/
    ├── client/          # HTTP clients for external services
    ├── config/          # Spring configuration beans
    ├── controller/      # REST controllers — thin layer, delegate to services
    ├── entity/          # JPA entities mapped to database tables
    ├── etl/             # CSV/TSV parsing, billing record assembly, batch writer
    ├── exception/       # Custom exceptions and global exception handler
    ├── listener/        # SQS event listeners (event-driven ingestion)
    ├── mapper/          # Converts between entities and domain models
    ├── model/           # Domain model records/classes (not persisted directly)
    ├── repository/      # Spring Data JPA repositories
    ├── service/         # Business logic, organised by domain
    ├── util/            # Shared utilities
    └── validation/      # Custom Jakarta Bean Validation annotations
```
