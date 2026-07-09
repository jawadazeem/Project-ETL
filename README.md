# Blueprint: AI-Powered Multi-Cloud FinOps Platform

### Author: Jawad Azeem

Blueprint is a microservices-based cloud billing intelligence platform that combines autonomous AI agents, machine learning forecasting, and automated anomaly detection to turn raw multi-cloud cost data into actionable insights.

### Live API: https://blueprint.jawadazeem.com

---

## Architecture

Four containerized services orchestrated with Docker Compose, deployed on AWS ECS behind a Cloudflare tunnel:

| Service | Stack | Purpose |
|---------|-------|---------|
| **Monolith** | Java 25, Spring Boot 3.5, PostgreSQL | ETL pipeline, REST API, Trace AI Agent |
| **Prediction** | Python, FastAPI, scikit-learn | Linear regression cost forecasting |
| **Audit** | Python, FastAPI, Pydantic | Duplicate charge and anomaly detection |
| **Notification** | TypeScript, Express, MongoDB | Alarm delivery via SES and Slack |

## AI & ML Capabilities

- **Trace AI Agent** — Natural language to validated PostgreSQL via Google Gemini. Generates, validates (JSQLParser), executes read-only queries, and returns plain-English answers.
- **Predictive Forecasting** — scikit-learn LinearRegression trained on historical billing periods to project future cloud spend.
- **Audit Engine** — Automated detection of duplicate charges and billing anomalies across multi-cloud datasets.
- **Organization Context (RAG)** — PGVector embeddings over uploaded contracts, policies, and ownership documents. Trace grounds its answers in organization-specific knowledge.

## Cloud & Infrastructure

- **AWS**: ECS (hosting), RDS (PostgreSQL), S3 (file storage), SQS (event-driven ingestion), SNS, SES
- **LocalStack**: Simulates S3/SQS for cost-free local and staging environments
- **Cloudflare Tunnel**: Secure routing to the deployment without exposing ports
- **GitHub Actions**: CI/CD pipeline — builds Docker images and deploys to ECS on push to main
- **Liquibase**: Database schema migrations with full version control

## Getting Started

```bash
mvn clean package        # Build
mvn test                 # Run tests
mvn spotless:apply       # Format (Google Java Format)
./scripts/docker/dev.sh  # Run locally with Docker
```

Version: **v2.0.0**