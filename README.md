# Blueprint: Telecom Billing Intelligence

### Author: Jawad Azeem
This is a Spring Boot application that performs a full ETL workflow on large telecom billing datasets. It transforms raw CSV data into structured intelligence, leveraging Autonomous AI Agents to bridge the gap between raw data and natural language insights.
A live version of the API is hosted on AWS.

### Access the Live API at: 
- ### https://blueprint.jawadazeem.com

## Functionality
- **Autonomous SQL Generation:** Utilizes Google Gemini to translate natural language questions into validated PostgreSQL queries for real-time data exploration.
- **ETL Workflow:** High-performance ingestion of CSV datasets into structured BillingRecord entities.
- **Event-Driven Processing:** SQS-triggered ingestion and S3-based file handling for high-concurrency environments.
- **REST API:** Secure exposure of both deterministic analytics and AI-generated insights.

## Technologies Used
- Java 25
- Spring Boot (Web, AI, Security, Data)
- AWS (ECS, RDS, CloudWatch, SQS, SNS, S3)
- Google Gemini GenAI
- Docker
- JUnit 5 & Mockito
- PostgreSQL & Liquibase

## Cloud Architecture
![Architectural Bluprint](images/architecture.svg)
Version: **v1.0.0 General Availability**
