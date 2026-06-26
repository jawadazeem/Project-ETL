# Organizational Structure — Azeem Corporation

## Company Overview

Azeem Corporation is a mid-market technology and financial services company with ~1,800 employees across 3 offices (DC,NYC, London). The technology organization runs approximately 500 cloud resources across AWS, GCP, and Azure, organized into 20 cloud accounts.

## Cloud Account Ownership

### AWS Accounts

| Account Name | Owner Team | Purpose | Cost Center |
|---|---|---|---|
| Production-Main | Platform Engineering | Primary production workloads (EC2, ECS, RDS, S3, Lambda) | CC-4100 |
| Production-Secondary | Platform Engineering | Secondary production region and overflow capacity | CC-4100 |
| Shared-Services | Platform Engineering | Cross-cutting infra: Route 53, CloudFront, NAT Gateway, ELB, monitoring | CC-4100 |
| Edge-Network | Platform Engineering | CDN, edge compute, and networking services | CC-4100 |
| QA-Testing | Platform Engineering | Test environments, CI runners, load testing | CC-4100 |
| ML-Training | Data Science & ML | SageMaker experiments, GPU instances, training pipelines | CC-4200 |
| Compliance-Audit | InfoSec & Compliance | Security tooling, audit logs, Redshift for compliance analytics | CC-4300 |
| Security-Ops | InfoSec & Compliance | GuardDuty, SecurityHub, SIEM, incident response | CC-4300 |
| DevOps-Central | DevOps | CI/CD pipelines, container registries, secrets management | CC-4400 |
| Internal-Tools | DevOps | Developer productivity tools, internal dashboards | CC-4400 |
| Customer-Portal | Backend Engineering | Customer-facing API services, frontend hosting | CC-4500 |
| Mobile-Backend | Backend Engineering | Mobile API services, push notification infra | CC-4500 |
| Partner-Integration | Backend Engineering | B2B integrations, API gateway, partner data exchange | CC-4500 |
| Marketing-Platform | Backend Engineering | Marketing automation, campaign infrastructure | CC-4500 |
| Data-Science | Data Science & ML | Analytics pipelines, data lake, ML experiments | CC-4200 |
| Finance-Systems | Finance Technology | Financial reporting, billing systems | CC-4600 |

### GCP Projects

| Account Name | Owner Team | Purpose | Cost Center |
|---|---|---|---|
| Analytics-Platform | Data Science & ML | BigQuery analytics, Dataflow pipelines, Vertex AI models | CC-4200 |
| Data-Science | Data Science & ML | Cloud SQL, GKE workloads, ML experimentation | CC-4200 |
| Staging-Env | Platform Engineering | GCP staging: Cloud Run, Cloud SQL, Cloud Spanner testing | CC-4100 |
| IoT-Platform | Data Science & ML | IoT data ingestion, Cloud SQL, Dataflow processing | CC-4200 |
| Customer-Portal | Backend Engineering | Cloud Run services, BigQuery for customer analytics | CC-4500 |
| Edge-Network | Platform Engineering | Cloud DNS, CDN, Pub/Sub event streaming | CC-4100 |
| Marketing-Platform | Backend Engineering | Cloud Run campaigns, Pub/Sub events, Vertex AI personalization | CC-4500 |
| Security-Ops | InfoSec & Compliance | Cloud Logging, Cloud Monitoring, security analytics | CC-4300 |
| Finance-Systems | Finance Technology | Cloud Run services, Cloud SQL for financial data | CC-4600 |
| Shared-Services | Platform Engineering | Cloud Monitoring, Cloud Logging, Cloud Storage backups | CC-4100 |
| Partner-Integration | Backend Engineering | Dataflow ETL, BigQuery warehousing, Pub/Sub events | CC-4500 |
| Production-Main | Platform Engineering | GCP production: Cloud SQL, Cloud Storage, Vertex AI | CC-4100 |
| Production-Secondary | Platform Engineering | GCP secondary region, Cloud Spanner, Dataflow | CC-4100 |
| Internal-Tools | DevOps | Cloud Run internal apps, Cloud Functions automation | CC-4400 |

### Azure Subscriptions

| Account Name | Owner Team | Purpose | Cost Center |
|---|---|---|---|
| DR-Recovery | Platform Engineering | Disaster recovery: Cosmos DB replication, SQL Database standby | CC-4100 |
| DevOps-Central | DevOps | Azure DevOps, AKS build agents, Container Registry | CC-4400 |
| Customer-Portal | Backend Engineering | AKS microservices, Front Door, Cosmos DB | CC-4500 |
| Production-Main | Platform Engineering | Azure Front Door, AKS workloads, Event Hubs | CC-4100 |
| Production-Secondary | Platform Engineering | Azure secondary: AKS, SQL Database, Blob Storage | CC-4100 |
| Security-Ops | InfoSec & Compliance | Microsoft Sentinel, Key Vault, security monitoring | CC-4300 |
| Finance-Systems | Finance Technology | Azure SQL Database, App Service, Key Vault | CC-4600 |
| Mobile-Backend | Backend Engineering | AKS services, Azure Cache for Redis, Event Hubs | CC-4500 |
| Data-Science | Data Science & ML | Azure ML experiments, Cosmos DB, Blob Storage | CC-4200 |
| Shared-Services | Platform Engineering | Azure Monitor, Front Door, Event Hubs, Service Bus | CC-4100 |
| Partner-Integration | Backend Engineering | App Service APIs, Event Hubs, SQL Database | CC-4500 |
| Internal-Tools | DevOps | AKS internal apps, Cosmos DB, Application Insights | CC-4400 |

## Team Structure

### Platform Engineering (Head: Sarah Chen)

- **Headcount:** 32
- **Responsibility:** Core infrastructure, networking, Kubernetes clusters, database administration, CDN, DR
- **Cloud accounts:** Production-Main, Production-Secondary, Shared-Services, Edge-Network, QA-Testing, Staging-Env, DR-Recovery
- **Primary providers:** AWS (production), GCP (staging), Azure (DR)
- **Budget:** $840,000/year cloud spend
- **Key services:** EC2, ECS, EKS, RDS, S3, CloudFront, Route 53, ELB, NAT Gateway, ElastiCache, Cloud SQL, Cloud Spanner, GKE, AKS, Front Door, Cosmos DB

### Data Science & ML (Head: Marcus Johnson)

- **Headcount:** 18
- **Responsibility:** Analytics, ML model training, data pipelines, IoT data processing, business intelligence
- **Cloud accounts:** Data-Science, ML-Training, Analytics-Platform, IoT-Platform
- **Primary providers:** GCP (BigQuery, Vertex AI), AWS (SageMaker, Redshift)
- **Budget:** $520,000/year cloud spend
- **Key services:** BigQuery, Vertex AI, Dataflow, Cloud Storage, EC2 (GPU), Redshift, Cloud SQL, Cloud Spanner, Firestore, Memorystore

### Backend Engineering (Head: David Park)

- **Headcount:** 26
- **Responsibility:** Customer-facing APIs, mobile backends, partner integrations, marketing automation
- **Cloud accounts:** Customer-Portal, Mobile-Backend, Partner-Integration, Marketing-Platform
- **Primary providers:** AWS (Lambda, ECS), GCP (Cloud Run), Azure (AKS)
- **Budget:** $380,000/year cloud spend
- **Key services:** Lambda, Cloud Run, ECS, AKS, DynamoDB, SQS, SNS, Pub/Sub, Event Hubs, CloudFront, Front Door, Cosmos DB

### DevOps (Head: Lisa Wang)

- **Headcount:** 12
- **Responsibility:** CI/CD pipelines, container registries, developer productivity, internal tools
- **Cloud accounts:** DevOps-Central, Internal-Tools
- **Primary providers:** AWS (EKS, DynamoDB), Azure (Azure DevOps, AKS)
- **Budget:** $240,000/year cloud spend
- **Key services:** EKS, AKS, Azure DevOps, Container Registry, DynamoDB, ElastiCache, Secrets Manager, Cloud Run, Cloud Functions, Key Vault

### InfoSec & Compliance (Head: Rachel Torres)

- **Headcount:** 10
- **Responsibility:** Security monitoring, compliance auditing, vulnerability management, incident response
- **Cloud accounts:** Security-Ops, Compliance-Audit
- **Primary providers:** AWS (GuardDuty, SecurityHub), Azure (Microsoft Sentinel), GCP (Cloud Logging)
- **Budget:** $280,000/year cloud spend
- **Key services:** EC2, ECS, EKS, Redshift, S3, Microsoft Sentinel, Cloud Logging, Cloud Monitoring, Cloud Armor, Secrets Manager, Key Vault, VPN Gateway

### Finance Technology (Head: James Miller)

- **Headcount:** 8
- **Responsibility:** Financial reporting systems, billing infrastructure, regulatory reporting
- **Cloud accounts:** Finance-Systems
- **Primary providers:** AWS (RDS, S3), Azure (SQL Database, App Service), GCP (Cloud SQL)
- **Budget:** $180,000/year cloud spend
- **Key services:** RDS, Redshift, S3, Cloud SQL, Cloud Run, Azure SQL Database, App Service, Key Vault, Front Door, Cosmos DB

## Cost Center Summary

| Cost Center | Team | Annual Cloud Budget | Primary Provider |
|---|---|---|---|
| CC-4100 | Platform Engineering | $840,000 | AWS |
| CC-4200 | Data Science & ML | $520,000 | GCP |
| CC-4300 | InfoSec & Compliance | $280,000 | AWS / Azure |
| CC-4400 | DevOps | $240,000 | AWS / Azure |
| CC-4500 | Backend Engineering | $380,000 | AWS / GCP |
| CC-4600 | Finance Technology | $180,000 | AWS / Azure |
| **Total** | | **$2,440,000** | |

## Tagging Standards

All cloud resources must be tagged with:

| Tag Key | Required | Example | Purpose |
|---|---|---|---|
| `team` | Yes | `platform-eng` | Cost allocation |
| `environment` | Yes | `prod`, `staging`, `dev`, `sandbox` | Environment identification |
| `cost-center` | Yes | `CC-4100` | Finance chargeback |
| `account` | Yes | `Production-Main` | Account-level tracking |
| `project` | No | `customer-portal-v2` | Project-level tracking |
| `owner` | No | `schen@azeemcorp.com` | Point of contact |

**Compliance note:** Untagged resources are flagged in the monthly FinOps review. Teams with >5% untagged spend receive a notification from the FinOps team.
